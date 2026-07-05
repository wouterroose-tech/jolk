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
        // The standard Jolk calling convention layout is:
        // For Methods: index 0 is Receiver (self)
        // For Closures: index 0 is Lexical Environment (captured frame)
        // In a method context, self is at index 0.
        if (frame != null && frame.getArguments().length > 0) {
            return frame.getArguments()[0];
        }
        return JolkNothing.INSTANCE;
    }
}