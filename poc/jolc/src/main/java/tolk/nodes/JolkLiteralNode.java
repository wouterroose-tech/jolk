package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public class JolkLiteralNode extends JolkExpressionNode {
    private final Object value;

    public JolkLiteralNode(Object value) {
        this.value = value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }
}