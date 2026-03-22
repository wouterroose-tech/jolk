package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/// # JolkFieldNode
///
/// Represents a field or constant declaration in the Jolk AST.
/// Unlike methods, fields are primarily state containers with an optional initializer.
public class JolkFieldNode extends JolkNode {
    private final String name;
    @Child private JolkNode initializer;

    public JolkFieldNode(String name, JolkNode initializer) {
        this.name = name;
        this.initializer = initializer;
    }

    public String getName() {
        return name;
    }

    public JolkNode getInitializer() {
        return initializer;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // Executing a field node typically means evaluating its initializer (e.g. for constants)
        if (initializer != null) {
            return initializer.executeGeneric(frame);
        }
        return null;
    }
}
