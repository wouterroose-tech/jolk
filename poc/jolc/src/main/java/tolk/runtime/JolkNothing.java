package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/// The singleton representation of Jolk's `Nothing` identity.
///
/// In Jolk, `null` is not a void reference but the singleton instance of the
/// `Nothing` type, allowing it to respond to messages.
@ExportLibrary(InteropLibrary.class)
public final class JolkNothing implements TruffleObject {

    /// The singleton instance of Nothing.
    public static final JolkNothing NOTHING = new JolkNothing();

    private JolkNothing() {}

    @ExportMessage
    boolean isNull() {
        return true;
    }
}