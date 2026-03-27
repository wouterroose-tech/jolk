package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/// # JolkMatch
///
/// Represents the runtime container for the `Match<T>` type.
///
/// In the AST interpreter, this class serves as a necessary, concrete data carrier to
/// pass the result of an `#instanceOf` check between execution nodes.
///
/// In a compiled context (either via the Graal JIT or a future AOT compiler), this
/// object is a zero-cost abstraction. The compiler performs "Monadic Flow Flattening,"
/// recognizing the pattern and optimizing away the object allocation entirely.
/// 
@ExportLibrary(InteropLibrary.class)
public final class JolkMatch implements TruffleObject {

    private final Object value;
    private final boolean isPresent;

    private JolkMatch(Object value, boolean isPresent) {
        this.value = value;
        this.isPresent = isPresent;
    }

    /// Creates a successful match containing a value.
    public static JolkMatch with(Object value) {
        return new JolkMatch(value, true);
    }

    /// Creates an empty match.
    public static JolkMatch empty() {
        return new JolkMatch(null, false);
    }

    public Object getValue() {
        return value;
    }

    public boolean isPresent() {
        return isPresent;
    }

    @ExportMessage
    public String toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return isPresent ? "Match(" + value + ")" : "Match.empty";
    }

    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal) {
        return new JolkMemberNames(new String[]{"ifPresent", "isPresent", "isEmpty"});
    }

    @ExportMessage
    public boolean isMemberInvocable(String member) {
        return switch (member) {
            case "ifPresent", "isPresent", "isEmpty" -> true;
            default -> false;
        };
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        switch (member) {
            case "ifPresent":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                if (isPresent) {
                    Object action = arguments[0];
                    // Pass the UNWRAPPED value to the closure
                    return InteropLibrary.getUncached().execute(action, value);
                }
                // If empty, absorb the message and return self (acting like Nothing)
                return this;
            case "isPresent":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return isPresent;
            case "isEmpty":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return !isPresent;
            default:
                throw UnknownIdentifierException.create(member);
        }
    }
}