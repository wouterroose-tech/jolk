package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/// # JolkType
/// 
/// Represents a Jolk type (meta-object) at runtime.
/// This object is what a class definition evaluates to.
/// 
@ExportLibrary(InteropLibrary.class)
public final class JolkType implements TruffleObject {

    private final String name;
    private final boolean isFinal;

    public JolkType(String name, boolean isFinal) {
        this.name = name;
        this.isFinal = isFinal;
    }

    @ExportMessage
    boolean isMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaQualifiedName() {
        return name;
    }

    @ExportMessage
    Object getMetaSimpleName() {
        return name;
    }

    @ExportMessage
    boolean isMetaInstance(Object instance) {
        // TODO: Implement instance checking
        return false;
    }

    @ExportMessage
    boolean hasMembers() throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    boolean isMemberReadable(String member) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    Object readMember(String member) throws UnknownIdentifierException {
        if (isMemberReadable(member)) {
            return this.isFinal;
        }
        throw UnknownIdentifierException.create(member);
    }

    /// Helper class to expose member names as a TruffleObject.
    @ExportLibrary(InteropLibrary.class)
    static final class MemberNames implements TruffleObject {
        private final String[] members;

        MemberNames(String[] members) {
            this.members = members;
        }

        @ExportMessage
        boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        long getArraySize() {
            return members.length;
        }

        @ExportMessage
        Object readArrayElement(long index) throws InvalidArrayIndexException {
            if (index < 0 || index >= members.length) throw InvalidArrayIndexException.create(index);
            return members[(int) index];
        }

        @ExportMessage
        boolean isArrayElementReadable(long index) {
            return index >= 0 && index < members.length;
        }
    }
}