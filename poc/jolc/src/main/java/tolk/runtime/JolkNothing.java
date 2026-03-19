package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/// # JolkNothing
/// 
/// Represents the `null` or `Nothing` value in Jolk.
///
/// This is a singleton `TruffleObject` that implements the `isNull` message from the
/// `InteropLibrary` to correctly represent the absence of a value to the polyglot engine
/// and other languages. It aligns with Jolk's philosophy of treating `null` as a
/// first-class identity.
///
@ExportLibrary(InteropLibrary.class)
public final class JolkNothing implements TruffleObject {

    /// The singleton instance of the `Nothing` value.
    public static final JolkNothing INSTANCE = new JolkNothing();

    private JolkNothing() {
        // private constructor to enforce singleton pattern
    }

    ///
    /// Reports that this object represents a `null` value.
    /// @return Always `true`.
    ///
    @ExportMessage
    public boolean isNull() {
        return true;
    }

    @ExportMessage
    public String toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return "null";
    }

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(boolean includeInternal) {
        return new MemberNames(new String[]{"hash", "toString", "isPresent", "isEmpty", "ifPresent", "ifEmpty", "project", "class", "instanceOf"});
    }

    @ExportMessage
    boolean isMemberInvocable(String member) {
        return switch (member) {
            case "hash", "toString", "isPresent", "isEmpty", "ifPresent", "ifEmpty", "project", "class", "instanceOf" -> true;
            default -> false;
        };
    }

    @ExportMessage
    Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        switch (member) {
            case "hash":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return 0;
            case "toString":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return "null";
            case "isPresent":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return false;
            case "isEmpty":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return true;
            case "ifPresent":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                // Action is ignored for Nothing
                return this;
            case "ifEmpty":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                InteropLibrary.getUncached().execute(arguments[0]);
                return this;
            case "project":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                // Project is ignored for Nothing
                return this;
            case "instanceOf":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                // Defaulting to Match.empty as Nothing is a singleton.
                return JolkMatch.empty();
            case "class":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                // MetaClass for Nothing not yet available in this context, returning self as placeholder
                return this;
            default:
                throw UnknownIdentifierException.create(member);
        }
    }

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