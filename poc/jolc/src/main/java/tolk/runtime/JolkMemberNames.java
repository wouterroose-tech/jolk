package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/// Helper class to expose member names as a TruffleObject.
@ExportLibrary(InteropLibrary.class)
final class JolkMemberNames implements TruffleObject {
    private final String[] members;

    public JolkMemberNames(String[] members) {
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