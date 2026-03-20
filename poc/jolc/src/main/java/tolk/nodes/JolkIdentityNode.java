package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "==")
public class JolkIdentityNode extends JolkExpressionNode {

    @Child private JolkNode leftNode;
    @Child private JolkNode rightNode;
    private final boolean negate;

    public JolkIdentityNode(JolkNode leftNode, JolkNode rightNode, boolean negate) {
        this.leftNode = leftNode;
        this.rightNode = rightNode;
        this.negate = negate;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object left = leftNode.executeGeneric(frame);
        Object right = rightNode.executeGeneric(frame);
        
        if (negate) {
            return left != right;
        } else {
            return left == right;
        }
    }
}
