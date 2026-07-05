package tolk.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Idempotent;
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

    // INDUSTRIAL OPTIMIZATION: Interned constants for identity comparison (==).
    private static final String OP_GT = ">".intern();
    private static final String OP_LT = "<".intern();
    private static final String OP_GE = ">=".intern();
    private static final String OP_LE = "<=".intern();

    public abstract String getOperator();

    /**
     * Specialized fast-path for primitive longs.
     */
    @Specialization(guards = "isGt()")
    protected boolean doGt(long l1, long l2) { return l1 > l2; }

    @Specialization(guards = "isLt()")
    protected boolean doLt(long l1, long l2) { return l1 < l2; }

    @Specialization(guards = "isGe()")
    protected boolean doGe(long l1, long l2) { return l1 >= l2; }

    @Specialization(guards = "isLe()")
    protected boolean doLe(long l1, long l2) { return l1 <= l2; }

    /// Handles boxed Number objects from the host or other nodes.
    /// 
    @Specialization(replaces = {"doGt", "doLt", "doGe", "doLe"})
    protected boolean doNumbers(Number n1, Number n2) {
        // Guided Coercion: If either is a Double, promote both to Double for the comparison.
        if (n1 instanceof Double || n2 instanceof Double) {
            double d1 = n1.doubleValue();
            double d2 = n2.doubleValue();
            String op = getOperator();
            if (op == OP_GT) return d1 > d2;
            if (op == OP_LT) return d1 < d2;
            if (op == OP_GE) return d1 >= d2;
            if (op == OP_LE) return d1 <= d2;
            throw new RuntimeException("Unsupported comparison operator: " + op);
        }
        // Otherwise, both are Longs (or can be treated as Longs)
        String op = getOperator();
        if (op == OP_GT) return n1.longValue() > n2.longValue();
        if (op == OP_LT) return n1.longValue() < n2.longValue();
        if (op == OP_GE) return n1.longValue() >= n2.longValue();
        if (op == OP_LE) return n1.longValue() <= n2.longValue();
        
        throw new RuntimeException("Unsupported comparison operator: " + op);
    }

    @Idempotent protected boolean isGt() { return getOperator() == OP_GT; }
    @Idempotent protected boolean isLt() { return getOperator() == OP_LT; }
    @Idempotent protected boolean isGe() { return getOperator() == OP_GE; }
    @Idempotent protected boolean isLe() { return getOperator() == OP_LE; }

    /**
     * Unified Messaging Fallback.
     */
    @Fallback
    protected Object doFallback(VirtualFrame frame, Object leftNode, Object rightNode,
                                @Cached JolkDispatchNode dispatchNode) {
        return dispatchNode.execute(frame, leftNode, getOperator(), new Object[]{rightNode});
    }
}