package tolk.nodes;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

/**
 * Node for writing to a local variable or parameter in the Truffle frame.
 * Supports lexical scoping by navigating up the environment chain via the
 * captured context stored at index 0 of the arguments.
 */
public class JolkWriteLocalVariableNode extends JolkNode {

    private final int index;
    private final int depth;
    @Child private JolkNode valueNode;

    public JolkWriteLocalVariableNode(int index, int depth, JolkNode valueNode) {
        this.index = index;
        this.depth = depth;
        this.valueNode = valueNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object value = valueNode.executeGeneric(frame);
        Frame targetFrame = getTargetFrame(frame);
        if (targetFrame != null) {
            // Jolk uses indexed slots in the frame for locals and parameters
            // aligned with the indices in the visitor's scope stack.
            targetFrame.setObject(index, value);
        }
        return value;
    }

    @ExplodeLoop
    private Frame getTargetFrame(VirtualFrame frame) {
        Frame current = frame;
        for (int i = 0; i < depth; i++) {
            Object[] args = current.getArguments();
            if (args.length > 0 && args[0] instanceof Frame next) {
                current = next;
            } else {
                return null;
            }
        }
        return current;
    }
}