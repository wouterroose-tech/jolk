package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import tolk.runtime.JolkNothing;

/// Represents a member (field or method) definition in a class.
@NodeInfo(language = "Jolk", description = "The abstract syntax tree node for a class member definition.")
@ExportLibrary(InteropLibrary.class)
public class JolkMemberNode extends JolkNode implements TruffleObject {

    private final String name;
    private final JolkNode body;
    private final String[] parameters;
    private final boolean isVariadic;
    private final boolean isState;

    public JolkMemberNode(String name, JolkNode body, String[] parameters, boolean isVariadic, boolean isState) {
        this.name = name;
        this.body = body;
        this.parameters = parameters;
        this.isVariadic = isVariadic;
        this.isState = isState;
    }

    public JolkMemberNode(String name) {
        this(name, new JolkEmptyNode(), new String[0], false, true);
    }
    
    public JolkMemberNode(String name, JolkNode body) {
        this(name, body, new String[0], false, true);
    }

    public String getName() {
        return name;
    }

    public JolkNode getBody() {
        return body;
    }

    public String[] getParameters() {
        return parameters;
    }

    public boolean isVariadic() {
        return isVariadic;
    }

    public boolean isState() {
        return isState;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return this;
    }

    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @ExportMessage
    public Object execute(Object[] arguments) {
        Object result = body.executeGeneric(null);
        return result == null ? JolkNothing.INSTANCE : result;
    }

    @Override
    public String toString() {
        return "Member(" + name + ")";
    }
}