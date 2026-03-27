package tolk.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public class JolkClosure implements TruffleObject {
    private final CallTarget callTarget;

    public JolkClosure(CallTarget callTarget) {
        this.callTarget = callTarget;
    }

    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @ExportMessage
    public Object execute(Object[] arguments) {
        return callTarget.call(arguments);
    }


    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal) {
        return new JolkMemberNames(new String[]{"apply", "catch", "finally", "try"});
    }

    @ExportMessage
    public boolean isMemberInvocable(String member) {
        return switch (member) {
            case "apply", "catch", "finally", "try" -> true;
            default -> false;
        };
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        switch (member) {
            case "apply":
                return execute(arguments);
            case "catch":
                if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                Object errorType = arguments[0];
                Object handler = arguments[1];
                try {
                    return execute(new Object[0]);
                } catch (RuntimeException | Error t) {
                    if (errorType instanceof Class<?> clazz) {
                         if (clazz.isInstance(t)) return InteropLibrary.getUncached().execute(handler, t);
                    } else if (InteropLibrary.getUncached().isMetaInstance(errorType, t)) {
                         return InteropLibrary.getUncached().execute(handler, t);
                    }
                    throw t;
                }
            case "finally":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                Object finalAction = arguments[0];
                try {
                    return execute(new Object[0]);
                } finally {
                    InteropLibrary.getUncached().execute(finalAction);
                }
            case "try":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                Object logic = arguments[0];
                Object resource = execute(new Object[0]);
                try {
                    return InteropLibrary.getUncached().execute(logic, resource);
                } finally {
                    if (resource != null && resource != JolkNothing.INSTANCE) {
                        InteropLibrary.getUncached().invokeMember(resource, "close");
                    }
                }
            default:
                throw UnknownIdentifierException.create(member);
        }
    }
}