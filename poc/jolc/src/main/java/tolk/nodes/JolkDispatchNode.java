package tolk.nodes;

import com.oracle.truffle.api.dsl.GenerateInline;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkInt;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;

/// # JolkDispatchNode
///
/// The `JolkDispatchNode` is responsible for dispatching messages (method calls)
/// to the receiver object. It acts as the gateway between the Jolk AST and the
/// object's behavior.
///
/// In Jolk, every interaction is a message send. This node handles the polymorphic
/// dispatch logic, leveraging the Truffle DSL to inline caches (ICs) for performance.
/// It primarily uses the `InteropLibrary` to interact with objects, ensuring strict
/// adherence to the defined protocols (including those of `JolkNothing`).
/// 
/// ### Specialization for `JolkNothing`
///
/// The `doNothing` specialization provides a high-performance, monomorphic "fast path" for
/// messages sent to the `JolkNothing.INSTANCE` singleton. In a message-passing system, the
/// absence of a value (`null`) is a common receiver, and optimizing for it is critical.
///
/// This specialization works by caching the `InteropLibrary` specific to the `JolkNothing`
/// type. Since `JolkNothing` is a singleton, this cache is always hit after the first
/// invocation, making subsequent dispatches extremely fast. The node simply delegates the
/// message (`invokeMember`) to the `JolkNothing` object itself, which defines how it
/// should respond to various selectors (typically by returning itself, absorbing the message).
///
/// ### Generic Dispatch and Polymorphic Inline Caches
///
/// The `doDispatch` specialization is the general-purpose, polymorphic fallback for any
/// object that is not `JolkNothing`. It leverages Truffle's Polymorphic Inline Caches (PICs)
/// to maintain high performance across different receiver types.
///
/// The `limit = "3"` directive instructs the Truffle DSL to create an inline cache that can
/// specialize for up to three distinct receiver types. Here's how it works:
///
/// 1.  **Monomorphic State**: On the first invocation with a new type (e.g., `JolkMetaClass`),
///     the node rewrites itself to include a fast `instanceof JolkMetaClass` check and caches
///     the appropriate `InteropLibrary`.
/// 2.  **Polymorphic State**: When a second or third new type is encountered, the node adds
///     more `instanceof` checks, creating a chain of fast paths.
/// 3.  **Megamorphic State**: If a fourth type is seen, the cache is considered "megamorphic."
///     The node transitions to a more generic (and slightly slower) dispatch mechanism that
///     can handle an unlimited number of types, typically using a hash map lookup on the
///     receiver's class.
///
/// This strategy ensures that common call sites with a few receiver types remain highly
/// optimized, while still correctly handling fully dynamic scenarios.
///
/// ### Node Inlining Strategy
///
/// Node inlining is currently explicitly disabled (`@GenerateInline(false)`) for this PoC.
/// While inlining reduces memory footprint and improves performance by combining nodes,
/// it requires the consuming nodes (like `JolkMessageSendNode`) to be refactored to use
/// the Truffle DSL's `@Cached` injection rather than manual `@Child` fields.
///
/// To prioritize architectural clarity and simplicity during the initial implementation phase,
/// we defer this optimization. It can be easily enabled later as part of the industrialization phase.
@GenerateInline(false)
public abstract class JolkDispatchNode extends Node {

    /// Executes the message dispatch.
    ///
    /// @param receiver The object receiving the message.
    /// @param selector The message name (selector).
    /// @param arguments The arguments passed to the message.
    /// @return The result of the message send.
    /// 
    /// @return The result of the message send.
    public abstract Object executeDispatch(Object receiver, String selector, Object[] arguments);

    /// ### Fast Path for `Nothing`
    /// This specialization creates a high-speed path for messages sent to `JolkNothing.INSTANCE`.
    /// By handling this singleton type directly, we avoid the overhead of a full dynamic dispatch.
    @Specialization(limit = "1")
    protected Object doNothing(JolkNothing receiver, String selector, Object[] arguments,
                                @CachedLibrary("receiver") InteropLibrary interop) {
        try {
            // The logic for how Nothing responds is correctly encapsulated in JolkNothing itself.
            // This specialization simply provides a direct, cached route to that logic.
            return interop.invokeMember(receiver, selector, arguments);
        } catch (UnsupportedMessageException | ArityException | UnknownIdentifierException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        }
    }

    /// ### Fast Path for Integers
    /// Handles raw Java Integers by routing messages to the JolkInt prototype.
    @Specialization
    protected Object doInt(Integer receiver, String selector, Object[] arguments,
                           @CachedLibrary(limit = "3") InteropLibrary interop) {
        Object member = JolkInt.INT_TYPE.lookupInstanceMember(selector);
        if (member != null) {
            Object[] argsWithReceiver = new Object[arguments.length + 1];
            argsWithReceiver[0] = receiver;
            if (arguments.length > 0) System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
            try {
                return interop.execute(member, argsWithReceiver);
            } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                throw new RuntimeException("Error executing #" + selector + " on Int", e);
            }
        }
        try {
            return interop.invokeMember(receiver, selector, arguments);
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        }
    }

    /// ### Generic Dispatch
    /// This is the fallback for any object that is not `JolkNothing`. It uses a polymorphic
    /// inline cache (`limit = "3"`) to handle different receiver types efficiently.
    @Specialization(replaces = {"doNothing", "doInt"}, limit = "3")
    protected Object doDispatch(Object receiver, String selector, Object[] arguments,
                                @CachedLibrary("receiver") InteropLibrary interop) {
        // The logic is identical, but this specialization handles any generic TruffleObject.
        try {
            return interop.invokeMember(receiver, selector, arguments);
        } catch (UnsupportedMessageException | ArityException | UnknownIdentifierException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        }
    }
}