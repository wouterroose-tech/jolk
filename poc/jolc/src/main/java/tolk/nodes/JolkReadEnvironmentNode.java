package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

/**
 * ### JolkReadEnvironmentNode
 * 
 * Reads the arguments array (the lexical environment) at a specific depth.
 * This is used to resolve the target activation for non-local returns (^).
 */
public final class JolkReadEnvironmentNode extends JolkNode {

    private final int depth;

    public JolkReadEnvironmentNode(int depth) {
        this.depth = depth;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        Object[] targetArgs = args;
        for (int i = 0; i < depth; i++) {
            targetArgs = (Object[]) targetArgs[0];
        }
        return targetArgs;
    }
}