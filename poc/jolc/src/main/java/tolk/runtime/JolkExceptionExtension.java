package tolk.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * ## JolkExceptionExtension
 *
 * Implements the Jolk "Extension" protocol for {@link java.lang.Throwable}.
 * This provides shim-less integration by allowing any Java exception to respond
 * to Jolk messages like `#message` and `#throw`.
 */
@ExportLibrary(value = InteropLibrary.class, receiverClass = Throwable.class)
public final class JolkExceptionExtension implements  JolkIntrinsicObject {

    private static final JolkExceptionExtension INSTANCE = new JolkExceptionExtension();

    /**
     * ### getJolkMetaClass
     * 
     * Returns the MetaClass identity for Jolk exceptions.
     */
    @Override
    public JolkMetaClass getJolkMetaClass() {
        return JolkException.EXCEPTION_TYPE;
    }

    @ExportMessage
    static boolean hasMembers(Throwable receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(Throwable receiver, boolean includeInternal) {
        return new JolkMemberNames(new String[]{
            "message", "throw", "class", "hash", "toString", 
            "isPresent", "isEmpty", "ifPresent", "ifEmpty"
        });
    }

    @ExportMessage
    static boolean isMemberInvocable(Throwable receiver, String member) {
        return switch (member) {
            case "message", "throw", "class", "hash", "toString", 
                 "isPresent", "isEmpty", "ifPresent", "ifEmpty" -> true;
            default -> false;
        };
    }

    @ExportMessage
    @TruffleBoundary
    static Object invokeMember(Throwable receiver, String member, Object[] arguments) 
            throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        
        switch (member) {
            case "message":
                return receiver.getMessage();
            case "throw":
                if (receiver instanceof RuntimeException re) throw re;
                if (receiver instanceof Error err) throw err;
                throw new RuntimeException(receiver);
            case "class":
                return JolkException.EXCEPTION_TYPE;
            case "hash":
                return (long) receiver.hashCode();
            case "toString":
                return receiver.toString();
            case "isPresent":
                return true;
            case "isEmpty":
                return false;
        }

        // Handle conditional flow control if provided
        if ("ifPresent".equals(member) && arguments.length > 0) {
            return INSTANCE.invokeIntrinsicMember(receiver, member, arguments, InteropLibrary.getUncached());
        }

        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    static boolean hasMetaObject(Throwable receiver) {
        return true;
    }

    @ExportMessage
    static Object getMetaObject(Throwable receiver) {
        return JolkException.EXCEPTION_TYPE;
    }
}