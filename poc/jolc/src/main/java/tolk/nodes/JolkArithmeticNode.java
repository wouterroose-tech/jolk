package tolk.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * ### JolkArithmeticNode
 * 
 * Implements the **Semantic Flattening** optimization for common arithmetic 
 * operations (+, -, *, /, %, **). It provides a fast path for primitive numeric 
 * types, bypassing the general message dispatch logic, while falling back 
 * to the [JolkDispatchNode] for non-primitive receivers or complex protocols.
 */
@NodeInfo(shortName = "arithmetic")
@NodeChild(value = "leftNode", type = JolkNode.class)
@NodeChild(value = "rightNode", type = JolkNode.class)
public abstract class JolkArithmeticNode extends JolkExpressionNode {

    protected final String operator;
    @Child protected JolkDispatchNode dispatchNode;

    public JolkArithmeticNode(String operator) {
        this.operator = operator;
        this.dispatchNode = JolkDispatchNode.create();
    }

    /**
     * Specialized fast-path for primitive longs. 
     * Truffle uses this to avoid boxing the results of child nodes.
     */
    @Specialization
    protected long doLongs(long l1, long l2) {
        return switch (operator) {
            case "+" -> l1 + l2;
            case "-" -> l1 - l2;
            case "*" -> l1 * l2;
            case "/" -> l1 / l2;
            case "%" -> l1 % l2;
            case "**" -> (long) Math.pow(l1, l2);
            default -> throw new RuntimeException("Unsupported long operator: " + operator);
        };
    }

    /**
     * Handles cases where one or both operands are already boxed (e.g. from Host Interop).
     */
    @Specialization(replaces = "doLongs")
    protected Object doNumbers(Number n1, Number n2) {
        long l1 = n1.longValue();
        long l2 = n2.longValue();
        return doLongs(l1, l2);
    }

    /**
     * Unified Messaging Fallback: Use the central dispatcher for non-numeric types 
     * (e.g. String concatenation or custom operator overloading).
     */
    @Fallback
    protected Object doFallback(VirtualFrame frame, Object left, Object right) {
        return dispatchNode.executeDispatch(frame, left, operator, new Object[]{right});
    }
}