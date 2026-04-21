package tolk.nodes;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;

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
        Frame targetFrame;
        if (depth == 0) {
            targetFrame = frame;
        } else if (depth == 1) {
            Object[] args = frame.getArguments();
            targetFrame = (args.length > 0 && args[0] instanceof Frame f) ? f : null;
        } else {
            targetFrame = getTargetFrame(frame, depth);
        }

        if (targetFrame != null) {
            if (value instanceof Long l) {
                targetFrame.setLong(index, l);
            } else if (value instanceof Boolean b) {
                targetFrame.setBoolean(index, b);
            } else {
                targetFrame.setObject(index, value);
            }
        }
        return value;
    }
}