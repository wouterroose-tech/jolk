package tolk.nodes;

import com.oracle.truffle.api.dsl.GenerateInline;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkObject;
import tolk.runtime.JolkMatch;
import tolk.runtime.JolkBoolean;
import tolk.runtime.JolkLong;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
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
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnsupportedMessageException | ArityException | UnknownIdentifierException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        }
    }

    /// ### Fast Path for Longs
    /// Handles raw Java Longs by routing messages to the JolkLong prototype.
    @Specialization
    protected Object doLong(Long receiver, String selector, Object[] arguments,
                           @CachedLibrary(limit = "3") InteropLibrary interop) {
        try {
            if (isObjectIntrinsic(selector)) {
                return dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
        } catch (JolkReturnException e) {
            throw e;
        } catch (Exception e) {
             throw new RuntimeException("Error executing #" + selector + " on Long", e);
        }

        Object member = JolkLong.LONG_TYPE.lookupInstanceMember(selector);
        if (member != null) {
            Object[] argsWithReceiver = new Object[arguments.length + 1];
            argsWithReceiver[0] = receiver;
            if (arguments.length > 0) System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
            try {
                return interop.execute(member, argsWithReceiver);
            } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                throw new RuntimeException("Error executing #" + selector + " on Long", e);
            }
        }
        try {
            Object result = interop.invokeMember((Object) receiver, selector, arguments);
            // Identity Restitution: We lift raw JVM nulls into the Nothing singleton.
            if (result == null) {
                return JolkNothing.INSTANCE;
            }
            return result;
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        }
    }

    /// ### Fast Path for Booleans
    /// Handles raw Java Booleans by routing messages to the JolkBoolean prototype.
    @Specialization
    protected Object doBoolean(Boolean receiver, String selector, Object[] arguments,
                               @CachedLibrary(limit = "3") InteropLibrary interop) {
        try {
            if (isObjectIntrinsic(selector)) {
                return dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
        } catch (JolkReturnException e) {
            throw e;
        } catch (Exception e) {
             throw new RuntimeException("Error executing #" + selector + " on Boolean", e);
        }
        Object member = JolkBoolean.BOOLEAN_TYPE.lookupInstanceMember(selector);
        if (member != null) {
            Object[] argsWithReceiver = new Object[arguments.length + 1];
            argsWithReceiver[0] = receiver;
            if (arguments.length > 0) System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
            try {
                return interop.execute(member, argsWithReceiver);
            } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                throw new RuntimeException("Error executing #" + selector + " on Boolean", e);
            }
        }
        try {
            Object result = interop.invokeMember((Object) receiver, selector, arguments);
            if (result == null) {
                return JolkNothing.INSTANCE;
            }
            return result;
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        }
    }

    /// ### Generic Dispatch
    /// This is the fallback for any object that is not `JolkNothing`. It uses a polymorphic
    /// inline cache (`limit = "3"`) to handle different receiver types efficiently.
    @Specialization(replaces = {"doNothing", "doLong", "doBoolean"}, limit = "3")
    protected Object doDispatch(Object receiver, String selector, Object[] arguments,
                                @CachedLibrary("receiver") InteropLibrary interop) {
        try {
            // Apply the Jolk Object Protocol to all objects
            if (isObjectIntrinsic(selector)) {
                return dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }

            Object result = interop.invokeMember(receiver, selector, arguments);
            
            // Identity Restitution Protocol:
            if (result == null) {
                return JolkNothing.INSTANCE;
            }
            return result;
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnsupportedMessageException | ArityException | UnknownIdentifierException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        }
    }

    boolean isObjectIntrinsic(String member) {
        return switch (member) {
            case "==", "!=", "~~", "!~", "??", "hash", "toString", "class", "instanceOf", "isPresent", "isEmpty", "ifPresent", "ifEmpty", "?", "? :", "?!", "?! :" -> true;
            default -> false;
        };
    }

    private Object dispatchObjectIntrinsic(Object receiver, String name, Object[] arguments, InteropLibrary interop) {
        InteropLibrary genericInterop = InteropLibrary.getUncached();
        try {
            switch (name) {
                case "==" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    // Identity short-circuit: same instance is always identical.
                    if (receiver == other) return true;
                    // Only use InteropLibrary.isIdentical if both are interoperable types.
                    // Otherwise, for distinct plain Java Objects, they are not identical.
                    if (receiver instanceof TruffleObject || other instanceof TruffleObject ||
                        receiver instanceof Boolean || other instanceof Boolean ||
                        receiver instanceof Byte || other instanceof Byte ||
                        receiver instanceof Short || other instanceof Short ||
                        receiver instanceof Integer || other instanceof Integer ||
                        receiver instanceof Long || other instanceof Long ||
                        receiver instanceof Float || other instanceof Float ||
                        receiver instanceof Double || other instanceof Double ||
                        receiver instanceof Character || other instanceof Character ||
                        receiver instanceof String || other instanceof String) {
                        return interop.isIdentical(receiver, other, genericInterop);
                    }
                    return false; // Distinct plain Java objects are not identical
                }
                case "!=" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    if (receiver == other) return false;
                    // Only use InteropLibrary.isIdentical if both are interoperable types.
                    // Otherwise, for distinct plain Java Objects, they are not identical.
                    if (receiver instanceof TruffleObject || other instanceof TruffleObject ||
                        receiver instanceof Boolean || other instanceof Boolean ||
                        receiver instanceof Byte || other instanceof Byte ||
                        receiver instanceof Short || other instanceof Short ||
                        receiver instanceof Integer || other instanceof Integer ||
                        receiver instanceof Long || other instanceof Long ||
                        receiver instanceof Float || other instanceof Float ||
                        receiver instanceof Double || other instanceof Double ||
                        receiver instanceof Character || other instanceof Character ||
                        receiver instanceof String || other instanceof String) {
                        return !interop.isIdentical(receiver, other, genericInterop);
                    }
                    return true; // Distinct plain Java objects are non-identical
                }
                case "~~" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    if (receiver instanceof Number n1 && other instanceof Number n2) {
                        return n1.longValue() == n2.longValue();
                    }
                    return receiver.equals(other);
                }
                case "!~" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    if (receiver instanceof Number n1 && other instanceof Number n2) {
                        return n1.longValue() != n2.longValue();
                    }
                    return !receiver.equals(other);
                }
                case "??" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    // Identity-Based Flow Control: execute fallback only if receiver is Nothing.
                    if (receiver == JolkNothing.INSTANCE) return genericInterop.execute(arguments[0]);
                    return receiver;
                }
                case "hash" -> { 
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    return (long) receiver.hashCode(); 
                }
                case "toString" -> { 
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    return receiver.toString(); 
                }
                case "isPresent" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof JolkMatch match) return match.isPresent();
                    return receiver != JolkNothing.INSTANCE;
                }
                case "isEmpty" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof JolkMatch match) return !match.isPresent();
                    return receiver == JolkNothing.INSTANCE;
                }
                case "ifPresent" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver == JolkNothing.INSTANCE) return JolkNothing.INSTANCE;
                    if (receiver instanceof JolkMatch match) {
                        if (match.isPresent()) {
                            Object val = match.getValue();
                            return genericInterop.execute(arguments[0], val == null ? JolkNothing.INSTANCE : val);
                        }
                        return JolkNothing.INSTANCE;
                    }
                    return genericInterop.execute(arguments[0], receiver);
                }
                case "ifEmpty" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver == JolkNothing.INSTANCE) return genericInterop.execute(arguments[0]);
                    if (receiver instanceof JolkMatch match) {
                        if (!match.isPresent()) return genericInterop.execute(arguments[0]);
                        return receiver;
                    }
                    return receiver;
                }
                case "class" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof JolkObject jo) return jo.getJolkMetaClass();
                    if (receiver instanceof Long || receiver instanceof Integer) return JolkLong.LONG_TYPE;
                    if (receiver instanceof Boolean) return JolkBoolean.BOOLEAN_TYPE;
                    return JolkNothing.NOTHING_TYPE;
                }
                case "instanceOf" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object type = arguments[0];
                    return (genericInterop.isMetaObject(type) && genericInterop.isMetaInstance(type, receiver)) ? JolkMatch.with(receiver) : JolkMatch.empty();
                }
                case "?" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver instanceof Boolean b && b) return genericInterop.execute(arguments[0]);
                    return receiver; // Binary branching returns receiver
                }
                case "?!" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver instanceof Boolean b && !b) return genericInterop.execute(arguments[0]);
                    return receiver;
                }
                case "? :" -> {
                    if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                    if (receiver instanceof Boolean b) {
                        return b ? genericInterop.execute(arguments[0]) : genericInterop.execute(arguments[1]);
                    }
                    return JolkNothing.INSTANCE;
                }
                case "?! :" -> {
                    if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                    if (receiver instanceof Boolean b) {
                        return !b ? genericInterop.execute(arguments[0]) : genericInterop.execute(arguments[1]);
                    }
                    return JolkNothing.INSTANCE;
                }
            }
        } catch (JolkReturnException e) {
            throw e;
        } catch (Throwable e) {
            if (e instanceof JolkReturnException re) throw re;
            throw new RuntimeException("Intrinsic dispatch failed: #" + name, e);
        }
        return JolkNothing.INSTANCE;
    }
}