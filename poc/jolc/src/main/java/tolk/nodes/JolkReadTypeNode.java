package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
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

    public JolkReadTypeNode(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        JolkLanguage lang = JolkLanguage.getLanguage(this);
        JolkContext context = lang.getContextReference().get(this);
        Object type = context.getDefinedClass(typeName);
        return type != null ? type : JolkNothing.INSTANCE;
    }
}