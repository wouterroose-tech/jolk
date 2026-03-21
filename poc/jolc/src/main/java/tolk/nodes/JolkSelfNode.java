package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import tolk.runtime.JolkNothing;

/// # JolkSelfNode
/// 
/// The AST node representing the `self` keyword in Jolk.
/// This node evaluates to the current instance (the receiver) 
/// in the context of a method or field access.
/// If `self` is used outside of an instance context, 
/// it returns `JolkNothing` to indicate the absence of a valid receiver.
/// ²
public class JolkSelfNode extends JolkExpressionNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        if (frame != null && frame.getArguments().length > 0) {
            return frame.getArguments()[0];
        }
        return JolkNothing.INSTANCE;
    }
}