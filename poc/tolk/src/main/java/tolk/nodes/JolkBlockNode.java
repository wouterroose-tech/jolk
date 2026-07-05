package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import tolk.runtime.JolkNothing;

/// ### JolkBlockNode
/// 
/// Executes a sequence of Jolk nodes. Returns the result of the last node.
///
public final class JolkBlockNode extends JolkNode {
    @Children private final JolkNode[] statements;

    public JolkBlockNode(JolkNode[] statements) {
        this.statements = statements;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object result = JolkNothing.INSTANCE;
        for (JolkNode node : statements) {
            result = node.executeGeneric(frame);
        }
        return result;
    }
}