package tolk.runtime;

import java.util.Collections;

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
@ExportLibrary(value = InteropLibrary.class, 
               receiverClass = Throwable.class, 
               delegateTo = JolkIntrinsicObject.class)
public final class JolkExceptionExtension {

    /** The MetaClass identity for Jolk exceptions. */
    public static final JolkMetaClass EXCEPTION_TYPE;

    static {
        EXCEPTION_TYPE = new JolkMetaClass("Exception", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
    }

    @ExportMessage
    static boolean hasMembers(Throwable receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(Throwable receiver, boolean includeInternal) {
        return new JolkMemberNames(new String[]{
            "throw", "class"
        });
    }

    @ExportMessage
    static boolean isMemberInvocable(Throwable receiver, String member) {
        return switch (member) {
            case "throw", "class" -> true;
            default -> false;
        };
    }

    @ExportMessage
    @TruffleBoundary
    static Object invokeMember(Throwable receiver, String member, Object[] arguments) 
            throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        
        switch (member) {
            case "throw":
                throw throwException(receiver);
            case "class":
                return EXCEPTION_TYPE;
        }

        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    static boolean hasMetaObject(Throwable receiver) {
        return true;
    }

    @ExportMessage
    static Object getMetaObject(Throwable receiver) {
        return EXCEPTION_TYPE;
    }

    /**
     * ### throwException
     * 
     * Perfroms a "sneaky throw" to propagate the original Throwable without 
     * wrapping it in a RuntimeException, preserving its identity and stack.
     */
    private static RuntimeException throwException(Throwable t) {
        JolkExceptionExtension.<RuntimeException>doThrow(t);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void doThrow(Throwable t) throws T {
        throw (T) t;
    }
}