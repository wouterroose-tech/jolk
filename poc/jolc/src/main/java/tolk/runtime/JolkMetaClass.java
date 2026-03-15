package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/// # JolkMetaClass
/// 
/// Represents a Jolk Type (meta-object) at runtime.
///
/// This is the Java implementation of the `MetaClass` concept defined in Jolk.
/// It acts as the first-class identity for Classes, Records, and Enums, enabling
/// meta-level messaging such as `#new` and type introspection.
/// 
@ExportLibrary(InteropLibrary.class)
public final class JolkMetaClass implements TruffleObject {

    private final String name;
    private final JolkFinality finality;

    public JolkMetaClass(String name, JolkFinality finality) {
        this.name = name;
        this.finality = finality;
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
        return finality == JolkFinality.FINAL;
    }

    @ExportMessage
    Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        if (finality == JolkFinality.FINAL) {
            return new MemberNames(new String[]{"isFinal"});
        }
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    boolean isMemberReadable(String member) throws UnsupportedMessageException {
        return finality == JolkFinality.FINAL && "isFinal".equals(member);
    }

    @ExportMessage
    Object readMember(String member) throws UnknownIdentifierException {
        // We expose the internal enum state as the boolean 'isFinal' property
        // to satisfy the object protocol.
        if ("isFinal".equals(member) && finality == JolkFinality.FINAL) {
            return true;
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
