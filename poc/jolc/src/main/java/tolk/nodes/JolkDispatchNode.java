package tolk.nodes;

import com.oracle.truffle.api.dsl.GenerateInline;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkMatch;
import tolk.runtime.JolkBoolean;
import tolk.runtime.JolkExceptionExtension;

import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.frame.VirtualFrame;
import tolk.runtime.JolkLong;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import tolk.runtime.JolkMetaClass;
import com.oracle.truffle.api.library.CachedLibrary;

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
@GenerateInline(false)
public abstract class JolkDispatchNode extends JolkNode { // Keep extending JolkNode

    public static final String[] INTRINSIC_MEMBERS = {
        "==", "!=", "~~", "!~", "??", "hash", "toString", "class", "instanceOf", "isPresent", "isEmpty", "ifPresent", "ifEmpty", "throw", "?", "? :", "?!", "?! :"
    };

    /**
     * ### create
     * 
     * Static factory method to instantiate the node implementation. This method 
     * delegates to the generated {@link JolkDispatchNodeGen} class.
     * 
     * @return An instance of the generated JolkDispatchNode.
     */
    public static JolkDispatchNode create() {
        return JolkDispatchNodeGen.create();
    }

    /// Executes the message dispatch.
    ///
    /// @param frame The current execution frame.
    /// @param receiver The object receiving the message.
    /// @param selector The message name (selector).
    /// @param arguments The arguments passed to the message.
    /// @return The result of the message send.
    public abstract Object executeDispatch(VirtualFrame frame, Object receiver, String selector, Object[] arguments);

    /**
     * This method is not intended to be called directly on a JolkDispatchNode.
     * It is marked as `final` to satisfy the requirement from {@link JolkNode} 
     * while shielding it from the Truffle DSL's specialization generator. 
     */
    @Override
    public final Object executeGeneric(VirtualFrame frame) {
        throw new UnsupportedOperationException("JolkDispatchNode is not designed to be executed directly via executeGeneric(VirtualFrame). Use executeDispatch instead.");
    }

    @Specialization(guards = "isNothing(receiver)")
    protected Object doNothing(Object receiver, String selector, Object[] arguments, // Maps to executeDispatch
                                @CachedLibrary("getNothing()") InteropLibrary interop) {
        if (isObjectIntrinsic(selector)) {
            return dispatchObjectIntrinsic(JolkNothing.INSTANCE, selector, arguments, interop);
        }

        try {
            // Identity Restitution Protocol: For messages not handled by intrinsics, 
            // Nothing absorbs the message by returning itself. We delegate to the 
            // instance via Interop to allow for guest-level extensibility.
            return lift(interop.invokeMember(JolkNothing.INSTANCE, selector, arguments));
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on Nothing", e);
        }
    }

    /**
     * ### isNothing
     * 
     * Guard used to identify if the receiver should be treated as the Jolk 
     * Nothing identity.
     * 
     * @param receiver The object to check.
     * @return true if the receiver is null or the JolkNothing instance.
     */
    protected static boolean isNothing(Object receiver) {
        return receiver == null || receiver == JolkNothing.INSTANCE;
    }

    protected JolkNothing getNothing() {
        return JolkNothing.INSTANCE;
    }

    /// ### Fast Path for Longs
    /// Handles raw Java Longs by routing messages to the JolkLong prototype.
    @Specialization
    protected Object doLong(Long receiver, String selector, Object[] arguments, // Maps to executeDispatch
                           @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        if (isObjectIntrinsic(selector)) {
            return dispatchObjectIntrinsic(receiver, selector, arguments, interop);
        }

        try {
            // 1. Prototype Lookup: Check for Jolk-defined arithmetic (e.g., +, -, *)
            Object member = JolkLong.LONG_TYPE.lookupInstanceMember(selector);
            if (member != null) {
                Object[] argsWithReceiver = new Object[arguments.length + 1];
                argsWithReceiver[0] = receiver;
                if (arguments.length > 0) {
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                }
                // Library Mismatch: Use a generic library to execute members on a specialized receiver.
                return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
            }

            // 2. Host Fallback: Dispatch to standard Java Long members (e.g. longValue, compareTo)
            return lift(interop.invokeMember(receiver, selector, arguments));

        } catch (JolkReturnException e) {
            throw e;
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on Long", e);
        }
    }

    /// ### Fast Path for Booleans
    /// Handles raw Java Booleans by routing messages to the JolkBoolean prototype. This ensures
    /// that boolean primitives can participate in Jolk's message-passing protocol.
    @Specialization
    protected Object doBoolean(Boolean receiver, String selector, Object[] arguments, // Maps to executeDispatch
                               @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        if (isObjectIntrinsic(selector)) {
            return dispatchObjectIntrinsic(receiver, selector, arguments, interop);
        }

        try {
            // 1. Prototype Lookup: Check for Jolk-defined logic (e.g., &&, ||, !)
            Object member = JolkBoolean.BOOLEAN_TYPE.lookupInstanceMember(selector);
            if (member != null) {
                Object[] argsWithReceiver = new Object[arguments.length + 1];
                argsWithReceiver[0] = receiver;
                if (arguments.length > 0) {
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                }
                // Library Mismatch: Use a generic library to execute members on a specialized receiver.
                return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
            }

            // 2. Host Fallback: Dispatch to standard Java Boolean members (if any)
            return lift(interop.invokeMember(receiver, selector, arguments));

        } catch (JolkReturnException e) {
            throw e;
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on Boolean", e);
        }
    }

    /**
     * ### doThrowable
     * 
     * Specialized **Intrinsic Fast Path** for host exceptions.
     * 
     * This specialization ensures that [java.lang.Throwable] instances prioritize 
     * Jolk's internal control-flow logic (like `#throw`) over standard Java 
     * member lookup. This is critical for maintaining **Shim-less Integration** 
     * where the language must control the stack unwinding process.
     */
    @Specialization
    protected Object doThrowable(Throwable receiver, String selector, Object[] arguments, // Maps to executeDispatch
                               @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        if (isObjectIntrinsic(selector)) {
            return dispatchObjectIntrinsic(receiver, selector, arguments, interop);
        }
        try {
            Object result = interop.invokeMember(receiver, selector, arguments);
            return lift(result);
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        }
    }

    /// ### Generic Dispatch
    /// This is the fallback for any object that is not `JolkNothing`. It uses a polymorphic
    /// inline cache (`limit = "3"`) to handle different receiver types efficiently.
    @Specialization(replaces = {"doNothing", "doLong", "doBoolean", "doThrowable"}, limit = "3") // Maps to executeDispatch
    protected Object doDispatch(Object receiver, String selector, Object[] arguments,
                                @CachedLibrary("receiver") InteropLibrary interop) {
        InteropLibrary uncached = InteropLibrary.getUncached();
        try {
            // Receiver Restitution: Handle raw Java null or Interop null as Jolk Nothing identity.
            if (receiver == null || interop.isNull(receiver)) {
                if (isObjectIntrinsic(selector)) {
                    return dispatchObjectIntrinsic(JolkNothing.INSTANCE, selector, arguments, uncached);
                }
                return lift(uncached.invokeMember(JolkNothing.INSTANCE, selector, arguments));
            }

            // Identity Restitution Protocol: Intercept intrinsic messages for all objects
            if (isObjectIntrinsic(selector)) {
                return dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }

            /**
             * ### Meta-Object Interceptor (#new)
             * 
             * Implements the **Unified Messaging** rule for object creation. If the receiver 
             * is a host [Class] (MetaObject) and the selector is `#new`, we map it 
             * directly to the Interop `instantiate` protocol to invoke the Java constructor.
             */
            if ("new".equals(selector) && interop.isMetaObject(receiver) && interop.isInstantiable(receiver)) {
                try {
                    return interop.instantiate(receiver, arguments);
                } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                    throw new RuntimeException("Failed to instantiate host object: " + receiver + " with arguments.", e);
                }
            }

            return lift(interop.invokeMember((Object) receiver, selector, arguments));
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnsupportedMessageException | ArityException | UnknownIdentifierException | UnsupportedTypeException e) {
            // Use the full class name to distinguish between Value wrappers and internal HostObjects.
            String receiverStr = (receiver == null) ? "null" : receiver.getClass().getName() + " [" + receiver + "]";
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiverStr, e);
        }
    }

    /**
     * ### isObjectIntrinsic
     *
     * Checks if a member name belongs to the Jolk Core Protocol. This method 
     * serves as the runtime implementation of the 
     * `extension ObjectExtension on java.lang.Object` declaration.
     *
     * @param member The selector name to check.
     * @return true if the selector is a Jolk intrinsic.
     */
    public static boolean isObjectIntrinsic(String member) {
        if (member == null) return false;
        return switch (member) {
            case "==", "!=", "~~", "!~", "??", "hash", "toString", "class", 
                 "instanceOf", "isPresent", "isEmpty", "ifPresent", "ifEmpty", 
                 "throw", "?", "? :", "?!", "?! :" -> true;
            default -> false;
        };
    }

    @TruffleBoundary
    public static Object dispatchObjectIntrinsic(Object receiver, String name, Object[] arguments, InteropLibrary interop) {
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
                    if (receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver)) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver;
                }
                case "hash" -> { 
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (isNothing(receiver)) return 0L;
                    return (long) receiver.hashCode();
                }
                case "toString" -> { 
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (isNothing(receiver)) return JolkNothing.INSTANCE;
                    return lift(receiver.toString());
                }
                case "isPresent" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof JolkMatch match) return match.isPresent();
                    return receiver != null && receiver != JolkNothing.INSTANCE && !interop.isNull(receiver);
                }
                case "isEmpty" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof JolkMatch match) return !match.isPresent();
                    return receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver);
                }
                case "ifPresent" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver)) return JolkNothing.INSTANCE;
                    if (receiver instanceof JolkMatch match) {
                        if (match.isPresent()) {
                            Object val = match.getValue();
                            Object result = genericInterop.execute(arguments[0], (val == null || genericInterop.isNull(val)) ? JolkNothing.INSTANCE : val);
                            return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                        }
                        return JolkNothing.INSTANCE;
                    }
                    Object result = genericInterop.execute(arguments[0], receiver);
                    return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                }
                case "ifEmpty" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver)) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    if (receiver instanceof JolkMatch match) {
                        if (!match.isPresent()) {
                            Object result = genericInterop.execute(arguments[0]);
                            return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                        }
                        return receiver;
                    }
                    return receiver;
                }
                case "throw" -> {
                    if (receiver instanceof Throwable t) {
                        JolkExceptionExtension.throwException(t);
                    }
                    throw new RuntimeException("The #throw selector can only be invoked on Throwable identities.");
                }
                case "class" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof Long || receiver instanceof Integer) return JolkLong.LONG_TYPE;
                    if (receiver instanceof Boolean) return JolkBoolean.BOOLEAN_TYPE;
                    if (receiver instanceof JolkMetaClass) return receiver;
                    // Identity Restitution: Use the interop protocol to resolve specific host classes
                    // (e.g. java.lang.RuntimeException) via our extensions or host defaults.
                    return interop.hasMetaObject(receiver) ? interop.getMetaObject(receiver) : JolkNothing.NOTHING_TYPE;
                }
                case "instanceOf" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object type = arguments[0];
                    return (genericInterop.isMetaObject(type) && genericInterop.isMetaInstance(type, receiver)) ? JolkMatch.with(receiver) : JolkMatch.empty();
                }
                case "?" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver instanceof Boolean b && b) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver; // Binary branching returns receiver
                }
                case "?!" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver instanceof Boolean b && !b) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver;
                }
                case "? :" -> {
                    if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                    if (receiver instanceof Boolean b) {
                        Object result = b ? genericInterop.execute(arguments[0]) : genericInterop.execute(arguments[1]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return JolkNothing.INSTANCE;
                }
                case "?! :" -> {
                    if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                    if (receiver instanceof Boolean b) {
                        Object result = !b ? genericInterop.execute(arguments[0]) : genericInterop.execute(arguments[1]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
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
        return null;
    }
}