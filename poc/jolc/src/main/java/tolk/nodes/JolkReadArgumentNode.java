package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
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
        Object[] targetArgs = getTargetArgs(frame, depth);
        if (targetArgs == null) return JolkNothing.INSTANCE;
        return (index < targetArgs.length) ? targetArgs[index] : JolkNothing.INSTANCE;
    }
}
