package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import tolk.language.JolkContext;
import tolk.language.JolkLanguage;
import tolk.runtime.JolkNothing;

/**
 * ### JolkReadTypeNode
 * 
 * Resolves a Meta-Object (Type) identity at runtime.
 * This node defer the lookup to the execution phase, allowing for forward
 * references and cross-unit resolution that cannot be performed at parse-time.
 */
public final class JolkReadTypeNode extends JolkExpressionNode {
    private final String typeName;
    @Child private JolkNode metaReceiver;

    public JolkReadTypeNode(String typeName) {
        this(null, typeName, null);
    }

    public JolkReadTypeNode(String typeName, JolkNode metaReceiver) {
        this(null, typeName, metaReceiver);
    }

    public JolkReadTypeNode(JolkLanguage language, String typeName, JolkNode metaReceiver) {
        this.typeName = typeName;
        this.metaReceiver = metaReceiver;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        JolkContext context = JolkLanguage.getLanguage(this).getContextReference().get(this);

        // Priority 1: Global Type Resolution
        Object type = context.getDefinedClass(typeName);
        if (type != null) return type;

        // Priority 2: Meta-Constant/Field Fallback (Message to Self's class)
        if (metaReceiver != null) {
            Object metaObj = metaReceiver.executeGeneric(frame);
            if (metaObj != JolkNothing.INSTANCE) {
                try {
                    return InteropLibrary.getUncached(metaObj).invokeMember(metaObj, typeName);
                } catch (UnsupportedMessageException | UnknownIdentifierException | ArityException | UnsupportedTypeException e) {
                    // Fall through to Nothing
                }
            }
        }

        return JolkNothing.INSTANCE;
    }
}