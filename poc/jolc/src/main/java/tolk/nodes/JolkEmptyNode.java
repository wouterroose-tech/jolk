package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import tolk.runtime.JolkNothing;

///
/// A node representing empty source code, which evaluates to Jolk's `null`.
/// This is used as a default return value for unimplemented visit methods in the visitor.
/// It allows the parser to produce a valid AST even when certain constructs are not yet implemented,
/// enabling incremental development of the language features.
/// 
public final class JolkEmptyNode extends JolkNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return JolkNothing.INSTANCE;
    }
}