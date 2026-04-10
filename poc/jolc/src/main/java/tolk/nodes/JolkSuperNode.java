package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Marker node for the `super` reserved keyword.
 *
 * This node should only appear as the receiver of a super message send,
 * and it should never be executed directly.
 */
public final class JolkSuperNode extends JolkExpressionNode {

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("Invalid use of 'super' without a selector.");
    }
}
