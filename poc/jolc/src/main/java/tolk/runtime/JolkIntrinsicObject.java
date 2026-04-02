package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

/// # JolkIntrinsicObject
/// 
/// Defines the common protocol for intrinsic Jolk objects that provide
/// access to their meta-awareness (MetaClass) and the standard object protocol.
/// 
public interface JolkIntrinsicObject {

    /// ### getJolkMetaClass
    /// 
    /// Returns the Jolk MetaClass associated with this instance.
    /// @return the meta object.
    /// 
    JolkMetaClass getJolkMetaClass();

    /// ### invokeIntrinsicMember
    /// 
    /// Dispatches standard Jolk Object Protocol messages.
    /// This ensures consistent behavior between JolkObject and JolkException.
    /// 
    /// @param receiver The object receiving the message.
    /// @param member The selector name.
    /// @param arguments The message arguments.
    /// @param interop Cached interop library.
    /// @return The result of the message send.
    /// 
    default Object invokeIntrinsicMember(Object receiver, String member, Object[] arguments, InteropLibrary interop) 
            throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
        
        switch (member) {
            case "==" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                Object other = arguments[0];
                if (receiver == other) return true;
                return interop.isIdentical(receiver, other, interop);
            }
            case "!=" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                Object other = arguments[0];
                if (receiver == other) return false;
                return !interop.isIdentical(receiver, other, interop);
            }
            case "~~" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return receiver.equals(arguments[0]);
            }
            case "!~" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return !interop.asBoolean(this.invokeIntrinsicMember(receiver, "~~", arguments, interop));
            }
            case "??" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
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
            case "ifPresent" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                Object action = arguments[0];
                Object result = interop.execute(action, receiver);
                return (result == null || interop.isNull(result)) ? JolkNothing.INSTANCE : result;
            }
            case "ifEmpty" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return receiver;
            }
            case "isPresent" -> {
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return true;
            }
            case "isEmpty" -> {
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return false;
            }
            case "class" -> {
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return getJolkMetaClass();
            }
            case "instanceOf" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                Object type = arguments[0];
                if (interop.isMetaInstance(type, receiver)) {
                    return JolkMatch.with(receiver);
                }
                return JolkMatch.empty();
            }
        }
        return null; // Not an intrinsic
    }
}