package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;

/// ### JolkIdentityNode
/// 
/// Implements identity comparison (`==` and `!=`).
/// Uses [InteropLibrary] to ensure semantic identity across boxed primitives and polyglot objects.

@NodeInfo(shortName = "==")
public class JolkIdentityNode extends JolkExpressionNode {

    @Child private JolkNode leftNode;
    @Child private JolkNode rightNode;
    private final boolean negate;

    public JolkIdentityNode(JolkNode leftNode, JolkNode rightNode, boolean negate) {
        this.leftNode = leftNode;
        this.rightNode = rightNode;
        this.negate = negate;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object left = leftNode.executeGeneric(frame);
        Object right = rightNode.executeGeneric(frame);
        
        // Optimization: identical references are always the same identity.
        // This also guards against interop crashes with non-interop objects (like in tests).
        if (left == right) return negate ? false : true;
        if (left == null || right == null) return negate ? true : false;

        // Use Interop identity checking to handle boxed primitives (e.g. large Longs) correctly.
        // We pass the uncached library as the third argument for the profiling node context.
        boolean identical = InteropLibrary.getUncached().isIdentical(left, right, InteropLibrary.getUncached());

        return negate ? !identical : identical;
    }
}
