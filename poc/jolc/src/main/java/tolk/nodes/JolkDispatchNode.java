package tolk.nodes;

import com.oracle.truffle.api.dsl.GenerateInline;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkStringExtension;
import tolk.runtime.JolkMatch;
import tolk.runtime.JolkBooleanExtension;
import tolk.runtime.JolkExceptionExtension;
import tolk.runtime.JolkArrayExtension;

import java.util.List;
import java.util.ArrayList;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.frame.VirtualFrame;
import tolk.runtime.JolkLongExtension;
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
    protected Object doNothing(VirtualFrame frame, Object receiver, String selector, Object[] arguments, 
                                @CachedLibrary("getNothing()") InteropLibrary interop) {
        try {
            // Identity Restitution Protocol: For messages not handled by intrinsics, 
            // Nothing absorbs the message by returning itself. We delegate to the 
            // instance via Interop to allow for guest-level extensibility.
            return lift(interop.invokeMember(JolkNothing.INSTANCE, selector, arguments));
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException e) {
            if (JolkMetaClass.isObjectIntrinsic(selector)) {
                return JolkMetaClass.dispatchObjectIntrinsic(JolkNothing.INSTANCE, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(JolkNothing.INSTANCE, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Nothing", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
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
    protected Object doLong(VirtualFrame frame, Long receiver, String selector, Object[] arguments, 
                           @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        try {
            // 1. Prototype Lookup: Check for Jolk-defined arithmetic extensions
            Object member = JolkLongExtension.LONG_TYPE.lookupInstanceMember(selector);
            if (member != null) {
                Object[] argsWithReceiver = new Object[arguments.length + 1];
                argsWithReceiver[0] = receiver;
                if (arguments.length > 0) {
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                }
                // Library Mismatch: Use a generic library to execute members on a specialized receiver.
                return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
            }
            
            // Identity-Based Flow Control: #?? on a non-null Long always returns the receiver.
            if (arguments.length == 1 && "??".equals(selector)) return receiver;

            // 2. Host Fallback: Dispatch to standard Java Long members (e.g. longValue, compareTo)
            return lift(interop.invokeMember(receiver, selector, arguments));

        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException e) {
            if (JolkMetaClass.isObjectIntrinsic(selector)) {
                return JolkMetaClass.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Long", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on Long", e);
        }
    }

    /// ### Fast Path for Booleans
    /// Handles raw Java Booleans by routing messages to the JolkBoolean prototype. This ensures
    /// that boolean primitives can participate in Jolk's message-passing protocol.
    @Specialization
    protected Object doBoolean(VirtualFrame frame, Boolean receiver, String selector, Object[] arguments, 
                               @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        try {
            // 1. Prototype Lookup: Check for Jolk-defined boolean extensions
            Object member = JolkBooleanExtension.BOOLEAN_TYPE.lookupInstanceMember(selector);
            if (member != null) {
                Object[] argsWithReceiver = new Object[arguments.length + 1];
                argsWithReceiver[0] = receiver;
                if (arguments.length > 0) {
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                }
                // Library Mismatch: Use a generic library to execute members on a specialized receiver.
                return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
            }

            // Identity-Based Flow Control: #?? on a non-null Boolean always returns the receiver.
            if (arguments.length == 1 && "??".equals(selector)) return receiver;

            // 2. Host Fallback: Dispatch to standard Java Boolean members (if any)
            return lift(interop.invokeMember(receiver, selector, arguments));

        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException e) {
            if (JolkMetaClass.isObjectIntrinsic(selector)) {
                return JolkMetaClass.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Boolean", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on Boolean", e);
        }
    }

    /// ### doString
    /// 
    /// Specialized **Fast Path** for host strings.
    /// 
    /// This specialization allows [java.lang.String] instances to participate 
    /// in the Jolk messaging protocol by prioritizing Jolk-native string 
    /// augmentations (defined in jolk.lang.String) before falling back to 
    /// standard Java methods.
    @Specialization
    protected Object doString(VirtualFrame frame, String receiver, String selector, Object[] arguments,
                             @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        try {
            // Shim-less Optimization: Direct dispatch for common String methods 
            // where Interop might treat the receiver as a value rather than an object.
            if (arguments.length == 0) {
                if ("length".equals(selector)) return (long) receiver.length();
                if ("isEmpty".equals(selector)) return receiver.isEmpty();
                if ("toUpperCase".equals(selector)) return lift(receiver.toUpperCase());
                if ("toLowerCase".equals(selector)) return lift(receiver.toLowerCase());
                if ("trim".equals(selector)) return lift(receiver.trim());
            }
            // Identity-Based Flow Control: #?? optimization for non-null Strings
            if (arguments.length == 1 && "??".equals(selector)) return receiver;

            if (arguments.length == 1 && "contains".equals(selector)) {
                Object arg = arguments[0];
                if (interop.isString(arg)) return receiver.contains(interop.asString(arg));
            }

            // 1. Prototype Lookup: Check for Jolk-defined string extensions (e.g., #match)
            Object member = JolkStringExtension.STRING_TYPE.lookupInstanceMember(selector);
            if (member != null) {
                Object[] argsWithReceiver = new Object[arguments.length + 1];
                argsWithReceiver[0] = receiver;
                if (arguments.length > 0) {
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                }
                return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
            }

            // 2. Host Fallback: Dispatch to standard java.lang.String members
            return lift(interop.invokeMember(receiver, selector, arguments));
        } catch (UnknownIdentifierException e) {
            if (JolkMetaClass.isObjectIntrinsic(selector)) {
                return JolkMetaClass.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on String", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on String", e);
        }
    }

    /// ### Fast Path for Lists
    /// Handles java.util.List instances by routing messages to the Jolk Array extension.
    @Specialization
    protected Object doList(VirtualFrame frame, List<?> receiver, String selector, Object[] arguments,
                           @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        try {
            // 1. Prototype Lookup: Check for Jolk-defined Array extensions (e.g., #at, #put)
            Object member = JolkArrayExtension.ARRAY_TYPE.lookupInstanceMember(selector);
            if (member != null) {
                Object[] argsWithReceiver = new Object[arguments.length + 1];
                argsWithReceiver[0] = lift(receiver); // Identity Restitution
                if (arguments.length > 0) {
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                }
                return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
            }

            // 2. Host Fallback: Dispatch to standard java.util.List members
            return lift(interop.invokeMember(receiver, selector, arguments));
        } catch (UnknownIdentifierException e) {
            if (JolkMetaClass.isObjectIntrinsic(selector)) {
                return JolkMetaClass.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on List", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on List", e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on List", e);
        }
    }

    /// ### doThrowable
    ///
    /// Specialized **Intrinsic Fast Path** for host exceptions.
    /// 
    /// This specialization ensures that [java.lang.Throwable] instances prioritize 
    /// Jolk's internal control-flow logic (like `#throw`) over standard Java 
    /// member lookup. This is critical for maintaining **Shim-less Integration** 
    /// where the language must control the stack unwinding process.
    @Specialization
    protected Object doThrowable(VirtualFrame frame, Throwable receiver, String selector, Object[] arguments, 
                               @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        try {
            Object result = interop.invokeMember(receiver, selector, arguments);
            return lift(result);
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException e) {
            if (JolkMetaClass.isObjectIntrinsic(selector)) {
                return JolkMetaClass.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Throwable", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Throwable", e);
            }
        }
    }

    /// ### Generic Dispatch
    /// 
    /// This is the fallback for any object that is not `JolkNothing`. It uses a polymorphic
    /// inline cache (`limit = "3"`) to handle different receiver types efficiently.
    @Specialization(replaces = {"doNothing", "doLong", "doBoolean", "doString", "doList", "doThrowable"}, limit = "3") // Maps to executeDispatch
    protected Object doDispatch(VirtualFrame frame, Object receiver, String selector, Object[] arguments,
                                @CachedLibrary("receiver") InteropLibrary interop) {
        InteropLibrary uncached = InteropLibrary.getUncached();
        Object unwrappedReceiver = unwrap(receiver); // Unwrap the receiver for type checks
        try {
            // Receiver Restitution: Handle raw Java null or Interop null as Jolk Nothing identity.
            if (unwrappedReceiver == null || uncached.isNull(unwrappedReceiver)) {
                if (JolkMetaClass.isObjectIntrinsic(selector)) {
                    return JolkMetaClass.dispatchObjectIntrinsic(JolkNothing.INSTANCE, selector, arguments, uncached);
                }
                try {
                    return lift(uncached.invokeMember(JolkNothing.INSTANCE, selector, arguments));
                } catch (UnknownIdentifierException e) {
                    // Identity Restitution: Fallback to host member heuristic for Nothing
                    return lift(dispatchHostMember(JolkNothing.INSTANCE, selector, arguments));
                }
            }

            // 3. Jolk Prototype Lookup (Megamorphic / Generic Path)
            // Check if the receiver matches a Jolk intrinsic prototype.
            JolkMetaClass meta = null;
            if (unwrappedReceiver instanceof Long || unwrappedReceiver instanceof Integer) meta = JolkLongExtension.LONG_TYPE;
            else if (unwrappedReceiver instanceof Boolean) meta = JolkBooleanExtension.BOOLEAN_TYPE;
            else if (unwrappedReceiver instanceof String) meta = JolkStringExtension.STRING_TYPE;
            else if (unwrappedReceiver instanceof List) meta = JolkArrayExtension.ARRAY_TYPE;

            if (meta != null) {
                Object member = meta.lookupInstanceMember(selector);
                if (member != null) {
                    Object[] argsWithReceiver = new Object[arguments.length + 1];
                    argsWithReceiver[0] = lift(receiver); // Identity Restitution
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                    return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
                }
            }

            /**
             * ### Meta-Object Interceptor (#new)
             * 
             * Implements the **Unified Messaging** rule for object creation. If the receiver 
             * is a host [Class] (MetaObject) and the selector is `#new`, we map it 
             * directly to the Interop `instantiate` protocol to invoke the Java constructor.
             */
            if ("new".equals(selector) && (receiver instanceof Class || interop.isMetaObject(receiver) || interop.isInstantiable(receiver))) {
                // Shim-less Interceptor: Route List.class, ArrayList.class, or the Jolk Array MetaClass 
                // to the specialized Array factory logic.
                if (receiver == List.class || receiver == ArrayList.class || receiver == JolkArrayExtension.ARRAY_TYPE) {
                    try {
                        return JolkArrayExtension.ARRAY_TYPE.invokeMember("new", arguments);
                    } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                        throw new RuntimeException("Failed to instantiate Jolk Array from host class: " + receiver, e);
                    }
                }
                
                try {
                    if (interop.isInstantiable(receiver)) {
                        return lift(interop.instantiate(receiver, arguments));
                    }
                    return lift(dispatchHostMember(receiver, selector, arguments));
                } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException | RuntimeException e) {
                    throw new RuntimeException("Failed to instantiate host object: " + receiver + " with arguments.", e);
                }
            }

            try {
                return lift(interop.invokeMember((Object) receiver, selector, arguments));
            } catch (UnknownIdentifierException e) {
                // Identity Restitution Protocol: Intrinsic messages act as a fallback 
                // for all objects that do not explicitly override them.
                if (JolkMetaClass.isObjectIntrinsic(selector)) {
                    return JolkMetaClass.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
                }
                // Impedance Resolution: Fallback to host member heuristic
                try {
                    return lift(dispatchHostMember(receiver, selector, arguments));
                } catch (UnknownIdentifierException ex) {
                    throw new RuntimeException("Message dispatch failed: #" + selector, e);
                }
            }
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
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

                    // Identity Congruence: intrinsic types match by value to ensure identity 
                    // is tied to value regardless of boxed storage.
                    if (receiver instanceof Number n1 && other instanceof Number n2) {
                        return n1.longValue() == n2.longValue();
                    }
                    if (receiver instanceof Boolean b1 && other instanceof Boolean b2) {
                        return b1.booleanValue() == b2.booleanValue();
                    }
                    if (receiver instanceof String s1 && other instanceof String s2) {
                        return s1.equals(s2);
                    }

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

                    // Identity Congruence: intrinsic types match by value
                    if (receiver instanceof Number n1 && other instanceof Number n2) {
                        return n1.longValue() != n2.longValue();
                    }
                    if (receiver instanceof Boolean b1 && other instanceof Boolean b2) {
                        return b1.booleanValue() != b2.booleanValue();
                    }
                    if (receiver instanceof String s1 && other instanceof String s2) {
                        return !s1.equals(s2);
                    }

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
                    if (isNothing(receiver)) return "null";
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
                    if (receiver instanceof Long || receiver instanceof Integer) return JolkLongExtension.LONG_TYPE;
                    if (receiver instanceof Boolean) return JolkBooleanExtension.BOOLEAN_TYPE;
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

    /**
     * ### dispatchHostMember
     * 
     * Implements the **Shim-less Integration** heuristic by attempting to map a Jolk 
     * selector to a native Java member on a host object. It attempts the exact name, 
     * followed by common Java Bean patterns (get/is/set) and public field access.
     * 
     * @param receiver The host object receiving the message.
     * @param selector The Jolk selector (e.g., "name").
     * @param arguments The call arguments.
     * @return The result of the invocation or the receiver in case of a setter.
     * @throws UnknownIdentifierException If no matching host member is found.
     */
    @TruffleBoundary
    private static Object dispatchHostMember(Object receiver, String selector, Object[] arguments) throws UnknownIdentifierException {
        InteropLibrary interop = InteropLibrary.getUncached();
        String capitalized = capitalize(selector);

        // 1. Try Method/Getter/Field candidates (exact, get, is)
        String[] candidates = {selector, "get" + capitalized, "is" + capitalized};
        for (String candidate : candidates) {
            try {
                if (interop.isMemberInvocable(receiver, candidate)) {
                    return interop.invokeMember(receiver, candidate, arguments);
                }
            } catch (UnknownIdentifierException e) {
                // continue
            } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                throw new RuntimeException(e);
            }
            
            // Impedance Resolution: Fallback to Java Reflection for types that 
            // Interop treats as values without members (String, Long, Boolean).
            Object reflected = tryInvokeViaReflection(receiver, candidate, arguments);
            if (reflected != null) return reflected;

            // Support for public fields (Impedance Resolution)
            if (arguments.length == 0 && interop.isMemberReadable(receiver, candidate)) {
                try {
                    return interop.readMember(receiver, candidate);
                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                    // continue
                }
            }
        }

        // 2. Try Setter candidate (Fluent Pattern: #name(val) -> returns receiver)
        if (arguments.length == 1) {
            String setter = "set" + capitalized;
            if (interop.isMemberInvocable(receiver, setter)) {
                try {
                    interop.invokeMember(receiver, setter, arguments);
                    return receiver; 
                } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        throw UnknownIdentifierException.create(selector);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @TruffleBoundary
    private static Object tryInvokeViaReflection(Object receiver, String methodName, Object[] arguments) {
        InteropLibrary interop = InteropLibrary.getUncached();
        
        // Impedance Resolution: Prepare unboxed arguments for Java Reflection.
        // Jolk identities (JolkLong, JolkBoolean) and polyglot types must be 
        // converted to standard Java types to match reflection signatures.
        Object[] unboxed = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            Object arg = arguments[i];
            if (arg == null || arg == JolkNothing.INSTANCE) unboxed[i] = null;
            else if (arg instanceof JolkLongExtension jl) unboxed[i] = JolkLongExtension.asLong(jl);
            else if (arg instanceof JolkBooleanExtension jb) unboxed[i] = JolkBooleanExtension.asBoolean(jb);
            else if (interop.isString(arg)) {
                try { unboxed[i] = interop.asString(arg); } catch (UnsupportedMessageException e) { unboxed[i] = arg; }
            } else if (interop.isNumber(arg)) {
                try { unboxed[i] = interop.asLong(arg); } catch (UnsupportedMessageException e) { unboxed[i] = arg; }
            } else {
                unboxed[i] = arg;
            }
        }

        try {
            // 1. Meta-Object Instantiation: If selector is #new and receiver is a Class, 
            // map directly to constructors to support Jolk's object creation protocol.
            if ("new".equals(methodName) && receiver instanceof Class<?> clazz) {
                for (java.lang.reflect.Constructor<?> c : clazz.getConstructors()) {
                    if (c.getParameterCount() == unboxed.length) {
                        try {
                            return lift(c.newInstance(unboxed));
                        } catch (Exception e) {
                            // continue to next candidate
                        }
                    }
                }
            }

            // 2. Member Invocation: Simple heuristic for Shim-less Integration.
            // Allows Jolk to reach native methods on types like String and Long 
            // which are often treated as "memberless" values by standard Interop.
            for (java.lang.reflect.Method m : receiver.getClass().getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == unboxed.length) {
                    try {
                        return lift(m.invoke(receiver, unboxed));
                    } catch (Exception e) {
                        // continue to next candidate
                    }
                }
            }
        } catch (SecurityException e) {
        }
        return null;
    }

    /**
     * ### matchArguments
     * 
     * Matches provided arguments against a parameter signature, handling 
     * Java varargs by wrapping trailing arguments into an array.
     */
    private static Object[] matchArguments(Class<?>[] types, boolean isVarArgs, Object[] args) {
        if (!isVarArgs) {
            return (types.length == args.length) ? args : null;
        }
        
        int fixedCount = types.length - 1;
        if (args.length < fixedCount) return null;

        Object[] result = new Object[types.length];
        System.arraycopy(args, 0, result, 0, fixedCount);

        // Wrap trailing arguments into the varargs array component
        Class<?> varArgType = types[fixedCount].getComponentType();
        int varArgLen = args.length - fixedCount;
        Object varArgsArray = java.lang.reflect.Array.newInstance(varArgType, varArgLen);
        for (int i = 0; i < varArgLen; i++) {
            java.lang.reflect.Array.set(varArgsArray, i, args[fixedCount + i]);
        }
        result[fixedCount] = varArgsArray;
        
        return result;
    }

    /**
     * ### coerceArguments
     * 
     * Implements **Guided Coercion** for the reflection boundary. It ensures that 
     * Jolk's 64-bit Longs are narrowed to match the target Java parameter types 
     * (int, short, byte) where necessary.
     */
    private static Object[] coerceArguments(Class<?>[] types, Object[] args) {
        Object[] coerced = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Class<?> target = types[i];
            if (arg instanceof Long l) {
                if (target == int.class || target == Integer.class) coerced[i] = l.intValue();
                else if (target == short.class || target == Short.class) coerced[i] = l.shortValue();
                else if (target == byte.class || target == Byte.class) coerced[i] = l.byteValue();
                else if (target == double.class || target == Double.class) coerced[i] = l.doubleValue();
                else if (target == float.class || target == Float.class) coerced[i] = l.floatValue();
                else coerced[i] = arg;
            } else {
                coerced[i] = arg;
            }
        }
        return coerced;
    }
}