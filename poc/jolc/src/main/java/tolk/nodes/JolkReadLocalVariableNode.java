package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

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
        VirtualFrame targetFrame = getTargetFrame(frame);
        // Access the indexed slot in the frame.
        return targetFrame.getObject(index);
    }

    @ExplodeLoop
    private VirtualFrame getTargetFrame(VirtualFrame frame) {
        VirtualFrame current = frame;
        for (int i = 0; i < depth; i++) {
            // Navigates lexical environment via context pointer at index 0 of arguments
            Object[] args = current.getArguments();
            if (args.length > 0 && args[0] instanceof VirtualFrame) {
                current = (VirtualFrame) args[0];
            }
        }
        return current;
    }
}