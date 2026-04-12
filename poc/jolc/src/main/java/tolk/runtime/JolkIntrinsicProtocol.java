package tolk.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import tolk.language.JolkLanguage;
import tolk.nodes.JolkReturnException;

import java.util.Set;

/**
 * # JolkIntrinsicProtocol
 *
 * Centralizes the dispatch logic for Jolk's core object protocol.
 * This prevents duplication of logic across JolkMetaClass and JolkDispatchNode.
 */
public final class JolkIntrinsicProtocol {

    public static final Set<String> INTRINSIC_MEMBERS = Set.of(
        "new", "catch", "finally", "throw", "==", "!=", "~~", "!~", "??", "hash", "toString", "class",
        "instanceOf", "isPresent", "isEmpty", "ifPresent", "ifEmpty", 
        "?", "? :", "?!", "?! :"
    );

    private JolkIntrinsicProtocol() {}

    public static boolean isObjectIntrinsic(String member) {
        return member != null && INTRINSIC_MEMBERS.contains(member);
    }

    @TruffleBoundary
    public static Object dispatchObjectIntrinsic(Object receiver, String name, Object[] arguments, InteropLibrary interop) {
        var context = JolkLanguage.getContext();
        Object unwrapped = receiver;
        // Impedance Resolution: Use the Context Environment to extract native host objects
        if (context != null && context.env.isHostObject(receiver)) {
            unwrapped = context.env.asHostObject(receiver);
        }
        InteropLibrary genericInterop = InteropLibrary.getUncached();
        try {
            switch (name) {
                case "throw" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (unwrapped instanceof Throwable t) {
                        JolkExceptionExtension.throwException(t);
                        return JolkNothing.INSTANCE;
                    }
                    throw new RuntimeException("The #throw selector can only be invoked on Throwable identities.");
                }
                case "==" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    if (receiver == other) return true;
                    if (receiver instanceof Number n1 && other instanceof Number n2) return n1.longValue() == n2.longValue();
                    if (receiver instanceof Boolean b1 && other instanceof Boolean b2) return b1.booleanValue() == b2.booleanValue();
                    if (receiver instanceof String s1 && other instanceof String s2) return s1.equals(s2);
                    if (receiver instanceof TruffleObject || other instanceof TruffleObject || isBoxed(receiver) || isBoxed(other)) {
                        return interop.isIdentical(receiver, other, genericInterop);
                    }
                    return false;
                }
                case "!=" -> {
                    Object eq = dispatchObjectIntrinsic(receiver, "==", arguments, interop);
                    return (eq instanceof Boolean b) ? !b : true;
                }
                case "~~" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    if (receiver instanceof Number n1 && other instanceof Number n2) return n1.longValue() == n2.longValue();
                    return receiver.equals(other);
                }
                case "!~" -> {
                    Object eq = dispatchObjectIntrinsic(receiver, "~~", arguments, interop);
                    return (eq instanceof Boolean b) ? !b : true;
                }
                case "??" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver)) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver;
                }
                case "hash" -> { 
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    return (receiver == null || receiver == JolkNothing.INSTANCE) ? 0L : (long) (int) receiver.hashCode();
                }
                case "toString" -> { 
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    return (receiver == null || receiver == JolkNothing.INSTANCE) ? "null" : receiver.toString();
                }
                case "isPresent" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof JolkMatch match) return match.isPresent();
                    return receiver != null && receiver != JolkNothing.INSTANCE && !interop.isNull(receiver);
                }
                case "isEmpty" -> {
                    return !((Boolean) dispatchObjectIntrinsic(receiver, "isPresent", arguments, interop));
                }
                case "ifPresent" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver)) return JolkNothing.INSTANCE;
                    Object val = (receiver instanceof JolkMatch match) ? match.getValue() : receiver;
                    if (val == null) return JolkNothing.INSTANCE;
                    Object result = genericInterop.execute(arguments[0], val);
                    return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                }
                case "ifEmpty" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if ((Boolean) dispatchObjectIntrinsic(receiver, "isEmpty", new Object[0], interop)) {
                        Object result = InteropLibrary.getUncached().execute(arguments[0]);
                        return (result == null || InteropLibrary.getUncached().isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver;
                }
                case "class" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof Long || receiver instanceof Integer) return JolkLongExtension.LONG_TYPE;
                    if (receiver instanceof Boolean) return JolkBooleanExtension.BOOLEAN_TYPE;
                    if (receiver instanceof String) return JolkStringExtension.STRING_TYPE;
                    if (unwrapped instanceof Throwable) return JolkExceptionExtension.EXCEPTION_TYPE;
                    if (receiver instanceof JolkMetaClass) return receiver;
                    if (receiver instanceof JolkObject jo) return jo.getJolkMetaClass();
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
                    return JolkNothing.INSTANCE;
                }
                case "?!" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver instanceof Boolean b && !b) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return JolkNothing.INSTANCE;
                }
                case "? :" -> {
                    if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                    if (receiver instanceof Boolean b) {
                        Object result = b 
                            ? genericInterop.execute(arguments[0]) 
                            : genericInterop.execute(arguments[1]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return JolkNothing.INSTANCE;
                }
                case "?! :" -> {
                    if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                    if (receiver instanceof Boolean b) {
                        Object result = !b 
                            ? genericInterop.execute(arguments[0]) 
                            : genericInterop.execute(arguments[1]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return JolkNothing.INSTANCE;
                }
            }
        } catch (JolkReturnException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException("Intrinsic dispatch failed: #" + name, e);
        }
        return null;
    }

    private static boolean isBoxed(Object obj) {
        return obj instanceof Boolean || obj instanceof Byte || obj instanceof Short ||
               obj instanceof Integer || obj instanceof Long || obj instanceof Float ||
               obj instanceof Double || obj instanceof Character || obj instanceof String;
    }
}