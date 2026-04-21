package tolk.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.NodeField;
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
@NodeField(name = "rightNode", type = JolkNode.class)
@NodeField(name = "operator", type = String.class)
public abstract class JolkLogicalNode extends JolkExpressionNode {

    public abstract String getOperator();
    public abstract JolkNode getRightNode();

    /**
     * Specialized fast-path for primitive booleans with short-circuiting.
     * Only the left operand is a NodeChild; the right is evaluated lazily.
     */
    @Specialization
    protected Object doLogical(VirtualFrame frame, boolean leftNode,
                               @Shared("logicalDispatch") @Cached JolkDispatchNode dispatchNode) {
        if ("&&".equals(getOperator())) {
            if (!leftNode) return false;
        } else if ("||".equals(getOperator())) {
            if (leftNode) return true;
        }

        Object right = getRightNode().executeGeneric(frame);
        if (right instanceof Boolean b2) {
            return b2; // Result determined by the right operand
        }
        
        // Fallback for right-hand side if it's not a boolean (Custom Messaging)
        return dispatchNode.execute(frame, this, leftNode, getOperator(), new Object[]{right});
    }

    /**
     * Unified Messaging Fallback: Handles Nothing identities and custom operator overloading.
     */
    @Fallback
    protected Object doFallback(VirtualFrame frame, Object leftNode,
                                @Shared("logicalDispatch") @Cached JolkDispatchNode dispatchNode) {
        // Jolk Messaging Fallback: If left is Nothing, logic fails to Nothing
        if (leftNode == JolkNothing.INSTANCE) return JolkNothing.INSTANCE;

        // Dispatch for custom operator overloading
        Object right = getRightNode().executeGeneric(frame);
        return dispatchNode.execute(frame, this, leftNode, getOperator(), new Object[]{right});
    }
}