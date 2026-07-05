package tolk.nodes;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;

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
        try {
            long val = valueNode.executeLong(frame);
            writeLong(frame, val);
            return val;
        } catch (UnexpectedResultException e) {
            return writeGeneric(frame, e.getResult());
        }
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        long val = valueNode.executeLong(frame);
        writeLong(frame, val);
        return val;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        boolean val = valueNode.executeBoolean(frame);
        writeBoolean(frame, val);
        return val;
    }

    @Override
    public TruffleString executeTruffleString(VirtualFrame frame) throws UnexpectedResultException {
        TruffleString val = valueNode.executeTruffleString(frame);
        writeGeneric(frame, val);
        return val;
    }

    private Object writeGeneric(VirtualFrame frame, Object value) {
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

    private void writeLong(VirtualFrame frame, long value) {
        Frame targetFrame;
        if (depth == 0) {
            targetFrame = frame;
        } else if (depth == 1) {
            Object[] args = frame.getArguments();
            targetFrame = (args.length > 0 && args[0] instanceof Frame f) ? f : null;
        } else {
            targetFrame = getTargetFrame(frame, depth);
        }
        if (targetFrame != null) targetFrame.setLong(index, value);
    }

    private void writeBoolean(VirtualFrame frame, boolean value) {
        Frame targetFrame = (depth == 0) ? frame : getTargetFrame(frame, depth);
        if (depth == 1) {
            Object[] args = frame.getArguments();
            targetFrame = (args.length > 0 && args[0] instanceof Frame f) ? f : null;
        }
        if (targetFrame != null) targetFrame.setBoolean(index, value);
    }
}
