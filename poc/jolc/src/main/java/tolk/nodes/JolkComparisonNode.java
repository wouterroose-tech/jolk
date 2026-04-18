package tolk.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * ### JolkComparisonNode
 * 
 * Implements optimized numeric comparisons (>, <, >=, <=) via **Semantic Flattening**.
 */
@NodeInfo(shortName = "comparison")
@NodeChild(value = "leftNode", type = JolkNode.class)
@NodeChild(value = "rightNode", type = JolkNode.class)
public abstract class JolkComparisonNode extends JolkExpressionNode {

    protected final String operator;

    public JolkComparisonNode(String operator) {
        this.operator = operator;
    }

    /**
     * Specialized fast-path for primitive longs.
     */
    @Specialization
    protected boolean doLongs(long l1, long l2) {
        return switch (operator) {
            case ">"  -> l1 > l2;
            case "<"  -> l1 < l2;
            case ">=" -> l1 >= l2;
            case "<=" -> l1 <= l2;
            default   -> throw new RuntimeException("Unsupported comparison operator: " + operator);
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
    protected Object doFallback(Object left, Object right) {
        return JolkDispatchNode.create().execute(null, left, operator, new Object[]{right});
    }
}