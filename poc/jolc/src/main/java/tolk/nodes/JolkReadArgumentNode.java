package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import tolk.runtime.JolkNothing;

/**
 * Reads an argument from the virtual frame by index.
 * Used for resolving method and closure parameters.
 */
public class JolkReadArgumentNode extends JolkNode {

    private final int index;

    public JolkReadArgumentNode(int index) {
        this.index = index;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        // The index is already the correct position in the frame arguments array.
        return (index < args.length) ? args[index] : JolkNothing.INSTANCE;
    }
}
