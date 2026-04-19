package tolk.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import tolk.runtime.JolkClosure;

/// # JolkClosureNode
///
/// Responsible for creating a [JolkClosure] at runtime.
/// It captures the current execution context (the frame arguments) to provide 
/// lexical scoping within the closure logic.
public class JolkClosureNode extends JolkNode {

    private final CallTarget callTarget;

    public JolkClosureNode(CallTarget callTarget) {
        this.callTarget = callTarget;
    }

    // The executeGeneric method is now in JolkClosure, which is instantiated by this node.
    // This node's role is to create the JolkClosure instance with the correct CallTarget and environment.
    @Override
    public Object executeGeneric(com.oracle.truffle.api.frame.VirtualFrame frame) {
        MaterializedFrame environment = (frame != null) ? frame.materialize() : null;
        return new JolkClosure(callTarget, environment);
    }
}