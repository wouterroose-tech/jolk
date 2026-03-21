package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import java.util.HashSet;
import java.util.Set;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/// # JolkObject
/// 
/// The base class for all objects in the Jolk Object Model (JoMoo).
/// This includes instances of classes defined in the language, as well as any built-in objects.
/// Currently, this is just a placeholder to establish the object model structure.
/// In the future, this class will be expanded to include fields, methods, and other features of the object model.
/// 
@ExportLibrary(InteropLibrary.class)
public class JolkObject implements TruffleObject {

    private final JolkMetaClass metaClass;
    private final Object[] data;

    public JolkObject(JolkMetaClass metaClass) {
        this.metaClass = metaClass;
        this.data = new Object[metaClass.getFieldCount()];
    }

    JolkMetaClass getJolkMetaClass() {
        return metaClass;
    }

    Object getFieldValue(String name) {
        int index = metaClass.getFieldIndex(name);
        return getFieldValue(index);
    }

    Object getFieldValue(int index) {
        if (index >= 0 && index < data.length) {
            return data[index];
        }
        return null;
    }

    void setFieldValue(String name, Object value) {
        int index = metaClass.getFieldIndex(name);
        setFieldValue(index, value);
    }

    void setFieldValue(int index, Object value) {
        if (index >= 0 && index < data.length) {
            data[index] = value;
        }
    }

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        Set<String> keys = new HashSet<>(metaClass.getInstanceMemberKeys());
        keys.add("==");
        keys.add("!=");
        keys.add("~~");
        keys.add("!~");
        keys.add("hash");
        keys.add("toString");
        keys.add("ifPresent");
        keys.add("ifEmpty");
        keys.add("isPresent");
        keys.add("isEmpty");
        keys.add("class");
        keys.add("instanceOf");
        return new JolkMemberNames(keys.toArray(new String[0]));
    }

    @ExportMessage
    boolean isMemberInvocable(String member) {
        // An instance can invoke a member if it's an Object intrinsic or an instance member on its class.
        return metaClass.hasInstanceMember(member) || isObjectIntrinsic(member);
    }

    @ExportMessage
    Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        // 1. Prioritize user-defined members for overridable selectors.
        if (metaClass.hasInstanceMember(member)) {
            Object instanceMember = metaClass.lookupInstanceMember(member);
            // Prepend 'this' (receiver) to arguments as Jolk instance members expect it
            Object[] argsWithReceiver = new Object[arguments.length + 1];
            argsWithReceiver[0] = this;
            if (arguments.length > 0) System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
            return InteropLibrary.getUncached().execute(instanceMember, argsWithReceiver);
        }

        // 2. Handle Object.jolk intrinsics and fallbacks.
        switch (member) {
            case "==" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return this == arguments[0];
            }
            case "!=" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return this != arguments[0];
            }
            case "~~" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                // Default equivalence (fallback) is identity.
                return this == arguments[0];
            }
            case "!~" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return !InteropLibrary.getUncached().asBoolean(this.invokeMember("~~", arguments));
            }
            case "hash" -> {
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return this.hashCode();
            }
            case "toString" -> {
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return "instance of " + metaClass.getMetaSimpleName();
            }
            case "ifPresent" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                Object action = arguments[0];
                return InteropLibrary.getUncached().execute(action, this);
            }
            case "ifEmpty" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return this; // Do nothing for non-null object
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
                return metaClass; 
            }
            case "instanceOf" -> {
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                Object type = arguments[0];
                if (InteropLibrary.getUncached().isMetaInstance(type, this)) {
                    return JolkMatch.with(this);
                }
                return JolkMatch.empty();
            }
        }

        throw UnknownIdentifierException.create(member);
    }

    private boolean isObjectIntrinsic(String member) {
        return switch (member) {
            case "==", "!=", "~~", "!~", "hash", "toString", "ifPresent", "ifEmpty", "isPresent", "isEmpty", "class", "instanceOf" -> true;
            default -> false;
        };
    }
}