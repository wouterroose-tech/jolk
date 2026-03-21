package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import tolk.language.JolkLanguage;
import tolk.runtime.JolkClosure;

/// Represents a closure definition `[ params -> body ]`.
@NodeInfo(language = "Jolk", description = "The node definition for a closure.")
public class JolkClosureNode extends JolkExpressionNode {

    private final JolkNode body;
    private final String[] parameters;
    private final boolean isVariadic;

    public JolkClosureNode(JolkNode body, String[] parameters, boolean isVariadic) {
        this.body = body;
        this.parameters = parameters;
        this.isVariadic = isVariadic;
    }

    public JolkClosureNode(JolkNode body) {
        this(body, new String[0], false);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        JolkLanguage language = getRootNode().getLanguage(JolkLanguage.class);
        JolkRootNode root = new JolkRootNode(language, body, "closure");
        return new JolkClosure(root.getCallTarget());
    }
}