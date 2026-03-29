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

    /**
     * ### JolkReturnNode
     *
     * Constructs a new return node.
     *
     * @param expression The expression to be evaluated and returned.
     * @param targetNode The node used to resolve the lexical environment of the target method.
     */
    public JolkReturnNode(JolkNode expression, JolkNode targetNode) {
        this.expression = expression;
        this.targetNode = targetNode;
    }

    /**
     * ### executeGeneric
     *
     * Executes the return operation. It evaluates the `expression` to determine the return 
     * value and evaluates the `targetNode` to resolve the identity of the target 
     * lexical environment (the arguments array of the home method).
     *
     * @param frame The current execution frame.
     * @return This method never returns normally; it always throws a `JolkReturnException`.
     * @throws JolkReturnException to initiate a non-local return that bubbles up to the 
     * matching `JolkRootNode`.
     */
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object value = expression.executeGeneric(frame);
        Object target = targetNode.executeGeneric(frame);
        throw new JolkReturnException(value, target);
    }
}