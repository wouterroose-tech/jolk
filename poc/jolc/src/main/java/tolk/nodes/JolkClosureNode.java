package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import tolk.runtime.JolkClosure;

public class JolkClosureNode extends JolkExpressionNode {

    @Child private JolkRootNode rootNode;

    public JolkClosureNode(JolkNode body) {
        this.rootNode = new JolkRootNode(null, body);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return new JolkClosure(rootNode.getCallTarget());
    }
}