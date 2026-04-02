package tolk.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * ## JolkException
 *
 * The fundamental exception type for the Jolk language. 
 * It extends {@link java.lang.RuntimeException} to allow unchecked propagation 
 * through the JVM stack, adhering to Jolk's design of eliminating checked exceptions.
 */
@ExportLibrary(InteropLibrary.class)
public class JolkException extends AbstractTruffleException implements TruffleObject, JolkIntrinsicObject {
    private static final long serialVersionUID = 1L;
    private final Object jolkMessage;

    /** The MetaClass identity for Jolk exceptions. */
    public static JolkMetaClass EXCEPTION_TYPE;

    public JolkException(Object jolkMessage) {
        super(String.valueOf(jolkMessage));
        this.jolkMessage = jolkMessage;
    }

    public Object getJolkMessage() {
        return jolkMessage;
    }

    @Override
    public JolkMetaClass getJolkMetaClass() {
        return EXCEPTION_TYPE;
    }

    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public boolean isMemberInvocable(String member) {
        return switch (member) {
            case "throw", "message", "class", "hash", "toString", 
                 "isPresent", "isEmpty", "ifPresent", "ifEmpty", 
                 "??", "==", "!=", "~~", "!~", "instanceOf" -> true;
            default -> false;
        };
    }

    @ExportMessage
    public boolean hasMetaObject() {
        return EXCEPTION_TYPE != null;
    }

    @ExportMessage
    public Object getMetaObject() {
        return EXCEPTION_TYPE;
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments,
                        @CachedLibrary("this") InteropLibrary lib) 
            throws UnsupportedMessageException, ArityException, UnsupportedTypeException, UnknownIdentifierException {
        switch (member) {
            case "throw":
                throw this;
            case "message":
                return jolkMessage;
        }

        // Handle common Jolk protocol via the Intrinsic Object interface
        Object intrinsicResult = invokeIntrinsicMember(this, member, arguments, lib);
        if (intrinsicResult != null) {
            return intrinsicResult;
        }

        // Jolk Error Protocol: Signal dispatch failure for unknown selectors.
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    @TruffleBoundary
    public String toDisplayString(boolean allowSideEffects) {
        return "Exception: " + String.valueOf(jolkMessage);
    }
}