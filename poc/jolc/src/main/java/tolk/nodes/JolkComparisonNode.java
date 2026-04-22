package tolk.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * ### JolkComparisonNode
 * 
 * Implements optimized numeric comparisons (>, <, >=, <=) via **Semantic Flattening**.
 */
@NodeInfo(shortName = "comparison")
@NodeChild(value = "leftNode", type = JolkNode.class)
@NodeChild(value = "rightNode", type = JolkNode.class)
@NodeField(name = "operator", type = String.class)
public abstract class JolkComparisonNode extends JolkExpressionNode {

    public abstract String getOperator();

    /**
     * Specialized fast-path for primitive longs.
     */
    @Specialization
    protected boolean doLongs(long l1, long l2) {
        return switch (getOperator()) {
            case ">"  -> l1 > l2;
            case "<"  -> l1 < l2;
            case ">=" -> l1 >= l2;
            case "<=" -> l1 <= l2;
            default   -> throw new RuntimeException("Unsupported comparison operator: " + getOperator());
        };
    }

    /**
     * Handles boxed Number objects from the host or other nodes.
     */
    @Specialization(replaces = "doLongs")
    protected boolean doNumbers(Number n1, Number n2) {
        return doLongs(n1.longValue(), n2.longValue());
    }

    /**
     * Unified Messaging Fallback.
     */
    @Fallback
    protected Object doFallback(VirtualFrame frame, Object leftNode, Object rightNode,
                                @Cached JolkDispatchNode dispatchNode) {
        return dispatchNode.execute(frame, leftNode, getOperator(), new Object[]{rightNode});
    }
}