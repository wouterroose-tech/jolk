package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
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

    public JolkObject(JolkMetaClass metaClass) {
        this.metaClass = metaClass;
    }

    JolkMetaClass getJolkMetaClass() {
        return metaClass;
    }

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        // In a real implementation, this would return the union of instance fields/methods
        // and the meta-class methods. For now, delegating to the meta-class.
        return metaClass.getMembers(includeInternal);
    }

    @ExportMessage
    boolean isMemberInvocable(String member) {
        // Check intrinsic members first or fallback to meta-class
        return switch (member) {
            case "==", "!=", "~~", "hash", "toString", "ifPresent", "ifEmpty", "isPresent", "isEmpty", "class", "instanceOf" -> true;
            default -> metaClass.isMemberInvocable(member);
        };
    }

    @ExportMessage
    Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        // 1. Try to find the member in the meta-class (method lookup/override)
        if (metaClass.isMemberInvocable(member)) {
            return metaClass.invokeMember(member, arguments);
        }

        // 2. Handle Object.jolk intrinsics
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
                // Default equivalence is identity
                return this == arguments[0];
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
}