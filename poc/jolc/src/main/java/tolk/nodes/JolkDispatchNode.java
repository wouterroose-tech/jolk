package tolk.nodes;

import tolk.runtime.JolkNothing;
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
public abstract class JolkDispatchNode extends Node {

    /* TODO
    This node is a candidate for node object inlining. 
    The memory footprint is estimated to be reduced from 28 to 9 byte(s). 
    Add @GenerateInline(true) to enable object inlining for this node or 
    @GenerateInline(false) to disable this warning. 
    Also consider disabling cached node generation with @GenerateCached(false) if all usages will be inlined. 
    This warning may be suppressed using @SuppressWarnings("truffle-inlining").
    */

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

    /// ### Generic Dispatch
    /// This is the fallback for any object that is not `JolkNothing`. It uses a polymorphic
    /// inline cache (`limit = "3"`) to handle different receiver types efficiently.
    @Specialization(replaces = "doNothing", limit = "3")
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