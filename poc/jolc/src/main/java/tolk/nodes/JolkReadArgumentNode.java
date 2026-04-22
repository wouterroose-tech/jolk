package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import tolk.runtime.JolkNothing;

/// # JolkReadArgumentNode
///
/// Reads an argument from the virtual frame by index.
/// Used for resolving method and closure parameters across lexical scopes.
public class JolkReadArgumentNode extends JolkNode {

    private final int index;
    private final int depth;

    public JolkReadArgumentNode(int index, int depth) {
        this.index = index;
        this.depth = depth;
    }

    /**
     * ### executeGeneric
     * 
     * Traverses the captured lexical environments based on the required depth 
     * and retrieves the argument at the specified index.
     */
    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object[] targetArgs;
        if (depth == 0) {
            targetArgs = frame.getArguments();
        } else if (depth == 1) {
            Object[] args = frame.getArguments();
            targetArgs = (args.length > 0 && args[0] instanceof Frame f) ? f.getArguments() : null;
        } else {
            targetArgs = getTargetArgs(frame, depth);
        }

        if (targetArgs == null || index >= targetArgs.length) {
            return JolkNothing.INSTANCE;
        }
        return lift(targetArgs[index]);
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        Object[] args = (depth == 0) ? frame.getArguments() : getTargetArgs(frame, depth);
        if (args != null && index < args.length && args[index] instanceof Long val) {
            return val;
        }
        throw new UnexpectedResultException(executeGeneric(frame));
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        Object[] args = (depth == 0) ? frame.getArguments() : getTargetArgs(frame, depth);
        if (args != null && index < args.length && args[index] instanceof Boolean val) {
            return val;
        }
        throw new UnexpectedResultException(executeGeneric(frame));
    }

    @Override
    public TruffleString executeTruffleString(VirtualFrame frame) throws UnexpectedResultException {
        Object[] args = (depth == 0) ? frame.getArguments() : getTargetArgs(frame, depth);
        if (args != null && index < args.length && args[index] instanceof TruffleString val) {
            return val;
        }
        throw new UnexpectedResultException(executeGeneric(frame));
    }
}
