package tolk.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * ### JolkUnaryNode
 * 
 * Implements optimized unary operations (! and -) via **Semantic Flattening**.
 */
@NodeInfo(shortName = "unary")
@NodeChild(value = "valueNode", type = JolkNode.class)
@NodeField(name = "operator", type = String.class)
public abstract class JolkUnaryNode extends JolkExpressionNode {

    public abstract String getOperator();

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
    protected Object doFallback(VirtualFrame frame, Object valueNode,
                                @Cached JolkDispatchNode dispatchNode) {
        return dispatchNode.execute(frame, valueNode, getOperator(), new Object[0]);
    }

    @Override
    public abstract Object executeGeneric(VirtualFrame frame);

    @Idempotent
    protected boolean isNot() {
        return "!".equals(getOperator());
    }

    @Idempotent
    protected boolean isNegation() {
        return "-".equals(getOperator());
    }
}