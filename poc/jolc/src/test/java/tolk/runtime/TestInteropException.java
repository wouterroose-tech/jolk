package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;

@ExportLibrary(InteropLibrary.class)
public class TestInteropException extends IllegalArgumentException implements TruffleObject {
    private static final long serialVersionUID = 1L;
    public TestInteropException(String message) {
        super(message);
    }
}