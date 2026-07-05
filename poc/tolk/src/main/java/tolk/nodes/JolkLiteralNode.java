package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;

public class JolkLiteralNode extends JolkExpressionNode {
    private final Object value;

    public JolkLiteralNode(Object value) {
        this.value = value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        if (value instanceof Long l) return l;
        throw new UnexpectedResultException(value);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        if (value instanceof Boolean b) return b;
        throw new UnexpectedResultException(value);
    }

    @Override
    public TruffleString executeTruffleString(VirtualFrame frame) throws UnexpectedResultException {
        if (value instanceof TruffleString ts) return ts;
        throw new UnexpectedResultException(value);
    }
}