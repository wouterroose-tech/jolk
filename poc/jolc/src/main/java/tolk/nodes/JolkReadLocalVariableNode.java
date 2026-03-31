package tolk.nodes;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import tolk.runtime.JolkNothing;

/**
 * Node for reading a local variable from the Truffle frame slots.
 * Unlike parameters, which are stored in the arguments array, local variables
 * are stored in indexed slots within the frame.
 */
public class JolkReadLocalVariableNode extends JolkNode {

    private final int index;
    private final int depth;

    public JolkReadLocalVariableNode(int index, int depth) {
        this.index = index;
        this.depth = depth;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Frame targetFrame = getTargetFrame(frame, depth);
        if (targetFrame == null) return JolkNothing.INSTANCE;
        // Access the indexed slot in the frame.
        return targetFrame.getObject(index);
    }
}