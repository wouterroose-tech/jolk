package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import tolk.runtime.JolkNothing;

@ExportLibrary(InteropLibrary.class)
public final class JolkMemberNode extends JolkNode implements TruffleObject {

    private final String name;
    private final JolkNode body;

    public JolkMemberNode(String name, JolkNode body) {
        this.name = name;
        this.body = body;
    }

    public JolkMemberNode(String name) {
        this(name, null);
    }
    public String getName() {
        return name;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return this;
    }

    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] arguments) {
        if (body != null) {
            Object result = body.executeGeneric(null);
            if (result != null) return result;
        }
        return JolkNothing.INSTANCE;
    }
    
    @Override
    public String toString() {
        return "Member(" + name + ")";
    }
}