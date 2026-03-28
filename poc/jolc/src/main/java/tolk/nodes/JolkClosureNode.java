package tolk.nodes;

import com.oracle.truffle.api.CallTarget;
import tolk.runtime.JolkClosure;

/// # JolkClosureNode
///
/// Responsible for creating a [JolkClosure] at runtime.
/// It captures the current execution context (the frame arguments) to provide 
/// lexical scoping within the closure logic.
public class JolkClosureNode extends JolkNode {

    private final CallTarget callTarget;
    private final String[] parameters;
    private final boolean isVariadic;

    public JolkClosureNode(CallTarget callTarget, String[] parameters, boolean isVariadic) {
        this.callTarget = callTarget;
        this.parameters = parameters;
        this.isVariadic = isVariadic;
    }

    // The executeGeneric method is now in JolkClosure, which is instantiated by this node.
    // This node's role is to create the JolkClosure instance with the correct CallTarget and environment.
    @Override
    public Object executeGeneric(com.oracle.truffle.api.frame.VirtualFrame frame) {
        Object[] environment = (frame != null) ? frame.getArguments() : null;
        return new JolkClosure(callTarget, environment);
    }
}