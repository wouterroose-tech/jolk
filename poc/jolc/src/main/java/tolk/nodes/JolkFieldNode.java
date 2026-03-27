package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/// # JolkFieldNode
///
/// Represents a field or constant declaration in the Jolk AST.
/// Unlike methods, fields are primarily state containers with an optional initializer
/// and a stability flag that establishes instance-level immutability.
public class JolkFieldNode extends JolkNode {
    private final String name;
    @Child private JolkNode initializer;
    private final boolean isStable;

    /// Creates a new field node that is mutable by default.
    public JolkFieldNode(String name, JolkNode initializer) {
        this(name, initializer, false);
    }

    public JolkFieldNode(String name, JolkNode initializer, boolean isStable) {
        this.name = name;
        this.initializer = initializer;
        this.isStable = isStable;
    }

    public String getName() {
        return name;
    }

    public JolkNode getInitializer() {
        return initializer;
    }

    /// Returns `true` if this field is marked as `stable` or is a `constant`.
    public boolean isStable() {
        return isStable;
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
