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
        // Index 0 is the receiver (this), so parameters start at index 1.
        return (index + 1 < args.length) ? args[index + 1] : JolkNothing.INSTANCE;
    }
}
