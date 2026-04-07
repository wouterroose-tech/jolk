package tolk.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * ### JolkUnaryNode
 * 
 * Implements optimized unary operations (! and -) via **Semantic Flattening**.
 */
@NodeInfo(shortName = "unary")
@NodeChild(value = "valueNode", type = JolkNode.class)
public abstract class JolkUnaryNode extends JolkExpressionNode {

    protected final String operator;
    @Child protected JolkDispatchNode dispatchNode;

    public JolkUnaryNode(String operator) {
        this.operator = operator;
        this.dispatchNode = JolkDispatchNode.create();
    }

    @Specialization(guards = "isNot()")
    protected boolean doBoolean(boolean value) {
        return !value;
    }

    @Specialization(guards = "isNegation()")
    protected long doLong(long value) {
        return -value;
    }

    @Specialization(replaces = "doLong", guards = "isNegation()")
    protected Object doNumber(Number value) {
        // Semantic Flattening for host-provided numbers
        return -value.longValue();
    }

    @Fallback
    protected Object doFallback(VirtualFrame frame, Object value) {
        return dispatchNode.executeDispatch(frame, value, operator, new Object[0]);
    }

    protected boolean isNot() {
        return "!".equals(operator);
    }

    protected boolean isNegation() {
        return "-".equals(operator);
    }
}