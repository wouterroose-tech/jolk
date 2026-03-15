package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
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
}