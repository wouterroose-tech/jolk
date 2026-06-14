package tolk.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/// Small helper node to create a JolkClosure for a method-reference
@NodeInfo(shortName = "methodRefClosure")
public final class JolkMethodRefClosureNode extends JolkNode {

    private final CallTarget callTarget;
    private final String selector;
    @Child private JolkNode receiverNode;

    public JolkMethodRefClosureNode(CallTarget callTarget, String selector, JolkNode receiverNode) {
        this.callTarget = callTarget;
        this.selector = selector;
        this.receiverNode = receiverNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        MaterializedFrame env = (frame != null) ? frame.materialize() : null;
        Object captured = (receiverNode != null) ? receiverNode.executeGeneric(frame) : null;
        tolk.runtime.JolkClosure closure = new tolk.runtime.JolkClosure(callTarget, env);
        // conservative default: expect binary operator (reduce-like) arity
        closure.setMethodReferenceMetadata(captured, selector, 2);
        return closure;
    }
}
