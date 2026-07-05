package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/// # JolkFieldNode
///
/// Represents a field or constant declaration in the Jolk AST.
/// Unlike methods, fields are primarily state containers with a type, an optional
/// initializer, and a stability flag that establishes instance-level immutability.
public class JolkFieldNode extends JolkNode {
    private final String name;
    private final String typeName;
    @Child private JolkNode initializer;
    private final boolean isStable;
    private final boolean isLazy;

    /// Convenience constructor for unit tests or untyped declarations.
    public JolkFieldNode(String name, JolkNode initializer) {
        this(name, "Object", initializer, false, false);
    }

    /// Convenience constructor for unit tests or untyped declarations with stability control.
    public JolkFieldNode(String name, JolkNode initializer, boolean isStable) {
        this(name, "Object", initializer, isStable, false);
    }

    // Creates a new field node that is mutable by default.
    public JolkFieldNode(String name, String typeName, JolkNode initializer) {
        this(name, typeName, initializer, false, false);
    }

    public JolkFieldNode(String name, String typeName, JolkNode initializer, boolean isStable) {
        this(name, "Object", initializer, isStable, false);
    }    
    
    public JolkFieldNode(String name, String typeName, JolkNode initializer, boolean isStable, boolean isLazy) {
        this.name = name;
        this.typeName = typeName;
        this.initializer = initializer;
        this.isStable = isStable;
        this.isLazy = isLazy;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public boolean isLazy() { // New getter
        return isLazy;
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
