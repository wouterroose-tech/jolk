package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/// # JolkMetaFieldAccessor
/// 
/// Implements reflective access for meta-level fields on a [JolkMetaClass].
///
@ExportLibrary(InteropLibrary.class)
public final class JolkMetaFieldAccessor implements TruffleObject {
    private final String fieldName;
    private final int index;
    public final boolean isStable;

    public JolkMetaFieldAccessor(String fieldName, int index, boolean isStable) {
        this.fieldName = fieldName;
        this.index = index;
        this.isStable = isStable;
    }

    @ExportMessage
    public boolean isExecutable() { return true; }

    @ExportMessage
    public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
        if (arguments.length < 1) throw ArityException.create(1, 2, arguments.length);
        
        if (!(arguments[0] instanceof JolkMetaClass receiver)) {
            throw UnsupportedTypeException.create(arguments, "Receiver must be a JolkMetaClass");
        }

        if (arguments.length == 1) {
            Object result = receiver.getMetaFieldValue(index);
            // Identity Restitution Protocol: Ensure no raw JVM null or Interop null leaks.
            return (result == null || (result != JolkNothing.INSTANCE && InteropLibrary.getUncached().isNull(result))) ? JolkNothing.INSTANCE : result;
        } else {
            if (isStable) {
                throw UnsupportedTypeException.create(arguments, "Cannot modify stable/constant meta field: " + fieldName);
            }
            Object value = arguments[1];
            receiver.setMetaFieldValue(index, value);
            return receiver;
        }
    }
}
