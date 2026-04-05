package tolk.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.util.Collections;

/**
 * ## JolkException
 *
 * The fundamental exception type for the Jolk language. 
 * It extends {@link java.lang.RuntimeException} to allow unchecked propagation 
 * through the JVM stack, adhering to Jolk's design of eliminating checked exceptions.
 */
@ExportLibrary(InteropLibrary.class)
public class JolkException extends AbstractTruffleException implements JolkIntrinsicObject {
    private static final long serialVersionUID = 1L;
    private final JolkMetaClass metaClass;
    private final Object[] data;
    private final Object jolkMessage;

    /** The MetaClass identity for Jolk exceptions. */
    public static final JolkMetaClass EXCEPTION_TYPE;

    static {
        EXCEPTION_TYPE = new JolkMetaClass("Exception", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
    }

    public JolkException(JolkMetaClass metaClass) {
        this(metaClass, null);
    }

    public JolkException(JolkMetaClass metaClass, Object[] args) {
        super(metaClass.name);
        this.metaClass = metaClass;
        this.data = new Object[metaClass.getFieldCount()];
        this.jolkMessage = metaClass.name;
        if (args != null && args.length == data.length) {
            System.arraycopy(args, 0, data, 0, data.length);
        } else {
            System.arraycopy(metaClass.getDefaultFieldValues(), 0, data, 0, data.length);
        }
    }

    public JolkException(Object jolkMessage) {
        this(EXCEPTION_TYPE, new Object[0]);
    }

    public Object getJolkMessage() {
        return jolkMessage;
    }

    Object getFieldValue(int index) {
        if (index >= 0 && index < data.length) return data[index];
        return null;
    }

    void setFieldValue(int index, Object value) {
        if (index >= 0 && index < data.length) data[index] = value;
    }

    @Override
    public JolkMetaClass getJolkMetaClass() {
        return metaClass;
    }

    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal) {
        java.util.Set<String> keys = new java.util.HashSet<>(metaClass.getInstanceMemberKeys());
        keys.addAll(java.util.Arrays.asList("throw", "message", "class", "hash", "toString", "isPresent", "isEmpty", "ifPresent", "ifEmpty", "??", "==", "!=", "~~", "!~", "instanceOf"));
        return new JolkMemberNames(keys.toArray(new String[0]));
    }

    @ExportMessage
    public boolean isMemberInvocable(String member) {
        return metaClass.hasInstanceMember(member) || 
               java.util.Arrays.asList("throw", "message", "class", "hash", "toString", "isPresent", "isEmpty", "ifPresent", "ifEmpty", "??", "==", "!=", "~~", "!~", "instanceOf").contains(member);
    }

    @ExportMessage
    public boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    public Object getMetaObject() {
        return EXCEPTION_TYPE;
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments,
                        @CachedLibrary(limit = "3") InteropLibrary interopLib) 
            throws UnsupportedMessageException, ArityException, UnsupportedTypeException, UnknownIdentifierException {
        
        if (metaClass.hasInstanceMember(member)) {
            Object instanceMember = metaClass.lookupInstanceMember(member);
            Object[] argsWithReceiver = new Object[arguments.length + 1];
            argsWithReceiver[0] = this;
            if (arguments.length > 0) System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
            Object result = interopLib.execute(instanceMember, argsWithReceiver);
            return result == null ? JolkNothing.INSTANCE : result;
        }

        switch (member) {
            case "throw":
                throw this;
            case "message":
                return jolkMessage;
        }

        // Handle common Jolk protocol via the Intrinsic Object interface
        Object intrinsicResult = invokeIntrinsicMember(this, member, arguments, interopLib);
        if (intrinsicResult != null) {
            return intrinsicResult;
        }

        // Jolk Error Protocol: Signal dispatch failure for unknown selectors.
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    public boolean isException() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    public String toDisplayString(boolean allowSideEffects) {
        return "Exception: " + String.valueOf(jolkMessage);
    }
}