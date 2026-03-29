package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.interop.TruffleObject;

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
        if (left == right) return negate ? false : true;
        if (left == null || right == null) return negate ? true : false;

        InteropLibrary interop = InteropLibrary.getUncached();
        boolean identical;

        // Only call interop.isIdentical if both operands are interop-compatible.
        // Otherwise, if they are not reference-equal, they are not identical.
        if (isInteropCompatible(left) && isInteropCompatible(right)) {
            identical = interop.isIdentical(left, right, interop);
        } else {
            // If they are not reference-equal and at least one is not interop-compatible,
            // then they are not identical in the Jolk sense.
            identical = false;
        }

        return negate ? !identical : identical;
    }

    /**
     * Checks if an object is compatible with Truffle's InteropLibrary for identity checks.
     * This includes TruffleObjects, primitive wrapper types, and String.
     */
    private boolean isInteropCompatible(Object obj) {
        return obj instanceof TruffleObject ||
               obj instanceof Boolean || obj instanceof Byte || obj instanceof Short ||
               obj instanceof Integer || obj instanceof Long || obj instanceof Float ||
               obj instanceof Double || obj instanceof Character || obj instanceof String;
    }
}
