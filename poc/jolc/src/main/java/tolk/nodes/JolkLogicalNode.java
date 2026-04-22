package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import tolk.runtime.JolkNothing;

/**
 * ### JolkLogicalNode
 * 
 * Implements short-circuiting logical operations (&&, ||).
 * Unlike arithmetic nodes, this node manually controls the execution of its 
 * children to preserve short-circuiting behavior.
 */
@NodeInfo(shortName = "logic")
public class JolkLogicalNode extends JolkExpressionNode {

    @Child private JolkNode leftNode;
    @Child private JolkNode rightNode;
    @Child private JolkDispatchNode dispatchNode;
    private final String operator;

    public JolkLogicalNode(JolkNode leftNode, JolkNode rightNode, String operator) {
        this.leftNode = leftNode;
        this.rightNode = rightNode;
        this.operator = operator;
        this.dispatchNode = JolkDispatchNodeGen.create();
    }

    public String getOperator() {
        return operator;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object left = leftNode.executeGeneric(frame);

        // Jolk Messaging Fallback: If left is Nothing, logic fails to Nothing
        if (left == JolkNothing.INSTANCE) return JolkNothing.INSTANCE;

        if (left instanceof Boolean) {
            boolean l = (boolean) left;
            if ("&&".equals(operator)) {
                if (!l) return false;
            } else if ("||".equals(operator)) {
                if (l) return true;
            }

            Object right = rightNode.executeGeneric(frame);
            if (right instanceof Boolean b2) {
                return b2;
            }
            // Fallback for right-hand side if it's not a boolean (Custom Messaging)
            return dispatchNode.execute(frame, left, operator, new Object[]{right});
        }
        
        // Dispatch for custom operator overloading
        Object right = rightNode.executeGeneric(frame);
        return dispatchNode.execute(frame, left, operator, new Object[]{right});
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        Object result = executeGeneric(frame);
        if (result instanceof Boolean) {
            return (boolean) result;
        }
        throw new UnexpectedResultException(result);
    }
}