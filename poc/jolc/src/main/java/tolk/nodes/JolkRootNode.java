package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import tolk.language.JolkLanguage;

///
/// The root node of a Jolk execution tree.
///
/// This node wraps the top-level [JolkNode] (typically the result of parsing a compilation unit)
/// and serves as the entry point for the Truffle runtime to execute the code. It is responsible
/// for bridging the language context with the AST execution.
///
public final class JolkRootNode extends RootNode {

    @Child
    private JolkNode bodyNode;
    private final String name;

    public JolkRootNode(JolkLanguage language, JolkNode bodyNode, String name) {
        super(language);
        this.bodyNode = bodyNode;
        this.name = name;
    }

    public JolkRootNode(JolkLanguage language, JolkNode bodyNode) {
        this(language, bodyNode, "root");
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return bodyNode.executeGeneric(frame);
    }

    @Override
    public String getName() {
        return name;
    }
}