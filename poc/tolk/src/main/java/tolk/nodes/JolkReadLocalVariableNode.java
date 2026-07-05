package tolk.nodes;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
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
        Frame targetFrame;
        if (depth == 0) {
            targetFrame = frame;
        } else if (depth == 1) {
            Object[] args = frame.getArguments();
            targetFrame = (args.length > 0 && args[0] instanceof Frame f) ? f : null;
        } else {
            targetFrame = getTargetFrame(frame, depth);
        }

        if (targetFrame == null) return JolkNothing.INSTANCE;

        if (targetFrame.isLong(index)) {
            return targetFrame.getLong(index);
        } else if (targetFrame.isBoolean(index)) {
            return targetFrame.getBoolean(index);
        }
        // If the variable is an object, assume it's already lifted or handle null efficiently.
        Object value = targetFrame.getObject(index);
        return (value == null) ? JolkNothing.INSTANCE : value;
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        Frame target = (depth == 0) ? frame : getTargetFrame(frame, depth);
        if (target != null && target.isLong(index)) {
            return target.getLong(index);
        }
        throw new UnexpectedResultException(executeGeneric(frame));
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        Frame target = (depth == 0) ? frame : getTargetFrame(frame, depth);
        if (target != null && target.isBoolean(index)) {
            return target.getBoolean(index);
        }
        throw new UnexpectedResultException(executeGeneric(frame));
    }

    @Override
    public TruffleString executeTruffleString(VirtualFrame frame) throws UnexpectedResultException {
        Frame target = (depth == 0) ? frame : getTargetFrame(frame, depth);
        Object value = (target != null) ? target.getObject(index) : null;
        if (value instanceof TruffleString ts) return ts;
        throw new UnexpectedResultException(executeGeneric(frame));
    }
}