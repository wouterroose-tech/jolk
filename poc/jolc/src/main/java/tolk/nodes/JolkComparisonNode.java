package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * ### JolkComparisonNode
 * 
 * Implements optimized numeric comparisons (>, <, >=, <=) via **Semantic Flattening**.
 */
@NodeInfo(shortName = "comparison")
public class JolkComparisonNode extends JolkExpressionNode {

    @Child private JolkNode leftNode;
    @Child private JolkNode rightNode;
    @Child private JolkDispatchNode dispatchNode;
    private final String operator;

    public JolkComparisonNode(JolkNode leftNode, String operator, JolkNode rightNode) {
        this.leftNode = leftNode;
        this.operator = operator;
        this.rightNode = rightNode;
        this.dispatchNode = JolkDispatchNode.create();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object left = leftNode.executeGeneric(frame);
        Object right = rightNode.executeGeneric(frame);

        if (left instanceof Number n1 && right instanceof Number n2) {
            long l1 = n1.longValue();
            long l2 = n2.longValue();
            
            return switch (operator) {
                case ">" -> l1 > l2;
                case "<" -> l1 < l2;
                case ">=" -> l1 >= l2;
                case "<=" -> l1 <= l2;
                default -> dispatchNode.execute(frame, left, operator, new Object[]{right});
            };
        }

        return dispatchNode.execute(frame, left, operator, new Object[]{right});
    }
}