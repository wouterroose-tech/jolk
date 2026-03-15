package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
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
}