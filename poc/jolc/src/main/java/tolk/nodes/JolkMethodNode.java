package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/// # JolkMethodNode
///
/// Represents a method definition in the Jolk AST.
/// It encapsulates the executable body and parameter metadata.
public class JolkMethodNode extends JolkNode {

    private final String name;
    @Child private JolkNode body;
    private final String[] parameters;
    private final boolean isVariadic;

    public JolkMethodNode(String name, JolkNode body, String[] parameters, boolean isVariadic) {
        this.name = name;
        this.body = body;
        this.parameters = parameters;
        this.isVariadic = isVariadic;
    }

    public String getName() {
        return name;
    }

    public JolkNode getBody() {
        return body;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return body.executeGeneric(frame);
    }
}
