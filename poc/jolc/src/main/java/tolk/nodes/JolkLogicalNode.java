package tolk.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import tolk.runtime.JolkNothing;

/**
 * ### JolkLogicalNode
 * 
 * Implements short-circuiting logical operations (&&, ||).
 * Unlike arithmetic nodes, this node manually controls the execution of its 
 * children to preserve short-circuiting behavior.
 */
@NodeInfo(shortName = "logical")
@NodeChild(value = "leftNode", type = JolkNode.class)
public abstract class JolkLogicalNode extends JolkExpressionNode {

    @Child protected JolkNode rightNode;
    @Child protected JolkDispatchNode dispatchNode;
    protected final String operator;

    public JolkLogicalNode(String operator, JolkNode rightNode) {
        this.operator = operator;
        this.rightNode = rightNode;
        this.dispatchNode = JolkDispatchNode.create();
    }

    /**
     * Specialized fast-path for primitive booleans with short-circuiting.
     * Only the left operand is a NodeChild; the right is evaluated lazily.
     */
    @Specialization
    protected Object doLogical(VirtualFrame frame, boolean left) {
        if ("&&".equals(operator)) {
            if (!left) return false;
        } else if ("||".equals(operator)) {
            if (left) return true;
        }

        Object right = rightNode.executeGeneric(frame);
        if (right instanceof Boolean b2) {
            return b2; // Result determined by the right operand
        }
        
        // Fallback for right-hand side if it's not a boolean (Custom Messaging)
        return dispatchNode.execute(frame, left, operator, new Object[]{right});
    }

    /**
     * Unified Messaging Fallback: Handles Nothing identities and custom operator overloading.
     */
    @Fallback
    protected Object doFallback(VirtualFrame frame, Object left) {
        // Jolk Messaging Fallback: If left is Nothing, logic fails to Nothing
        if (left == JolkNothing.INSTANCE) return JolkNothing.INSTANCE;

        // Dispatch for custom operator overloading
        Object right = rightNode.executeGeneric(frame);
        return dispatchNode.execute(frame, left, operator, new Object[]{right});
    }
}