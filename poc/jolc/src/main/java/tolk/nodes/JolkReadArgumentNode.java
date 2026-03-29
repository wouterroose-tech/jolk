package tolk.nodes;

import com.oracle.truffle.api.frame.Frame;
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
        Object[] args = frame.getArguments();
        Object[] targetArgs = args;

        // The compiler will unroll this loop because depth is constant
        for (int i = 0; i < depth; i++) {
            // Navigate through the VirtualFrame/MaterializedFrame stored at index 0
            Object env = targetArgs[0];
            targetArgs = (env instanceof Frame f) ? f.getArguments() : (Object[]) env;
        }

        return (index < targetArgs.length) ? targetArgs[index] : JolkNothing.INSTANCE;
    }
}
