package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * ### JolkReturnNode
 * 
 * Represents an explicit return statement (`^ expression`).
 * It evaluates the expression and throws a [JolkReturnException] to unwind the stack.
 */
public final class JolkReturnNode extends JolkNode {
    @Child private JolkNode expression;
    @Child private JolkNode targetNode;

    public JolkReturnNode(JolkNode expression, JolkReadEnvironmentNode targetNode) {
        this.expression = expression;
        this.targetNode = targetNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object value = expression.executeGeneric(frame);
        Object target = targetNode.executeGeneric(frame);
        throw new JolkReturnException(value, target);
    }
}