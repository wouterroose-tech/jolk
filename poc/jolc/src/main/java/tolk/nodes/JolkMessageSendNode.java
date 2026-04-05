package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "msg")
public class JolkMessageSendNode extends JolkExpressionNode {

    @Child private JolkNode receiverNode;
    @Children private final JolkNode[] argumentNodes;
    private final String selector;
    
    @Child private JolkDispatchNode dispatchNode;

    public JolkMessageSendNode(JolkNode receiverNode, String selector, JolkNode[] argumentNodes) {
        this.receiverNode = receiverNode;
        this.selector = selector;
        this.argumentNodes = argumentNodes;
        this.dispatchNode = JolkDispatchNode.create();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object receiver = receiverNode.executeGeneric(frame);
        Object[] args = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            args[i] = argumentNodes[i].executeGeneric(frame);
        }
        
        return dispatchNode.executeDispatch(frame, receiver, selector, args);
    }
}