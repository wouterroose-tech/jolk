package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "tryWithResource")
public class JolkTryWithResourceNode extends JolkExpressionNode {

    @Child private JolkNode resourceProviderNode;
    @Child private JolkNode logicNode;
    @Child private JolkNode catchNode;
    @Child private JolkDispatchNode dispatchNode;

    public JolkTryWithResourceNode(JolkNode resourceProviderNode, JolkNode logicNode, JolkNode catchNode) {
        this.resourceProviderNode = resourceProviderNode;
        this.logicNode = logicNode;
        this.catchNode = catchNode;
        this.dispatchNode = JolkDispatchNode.create();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object resourceProvider = resourceProviderNode.executeGeneric(frame);
        Object logic = logicNode.executeGeneric(frame);

        try {
            return dispatchNode.execute(frame, resourceProvider, "try", new Object[]{logic});
        } catch (JolkReturnException e) {
            throw e;
        } catch (Throwable throwable) {
            if (catchNode != null) {
                Object handler = catchNode.executeGeneric(frame);
                try {
                    return InteropLibrary.getUncached().execute(handler, new Object[]{JolkNode.lift(throwable)});
                } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                    throw new RuntimeException("Failed to execute catch handler", e);
                }
            }
            throw throwable;
        }
    }
}
