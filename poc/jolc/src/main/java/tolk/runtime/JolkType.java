package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/// 
/// Represents a Jolk type (meta-object) at runtime.
/// This object is what a class definition evaluates to.
/// 
@ExportLibrary(InteropLibrary.class)
public final class JolkType implements TruffleObject {

    private final String name;

    public JolkType(String name) {
        this.name = name;
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
    boolean hasMembers() {
        // TODO 
        // An empty class has no members.
        return false;
    }

    @ExportMessage
    Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }
}