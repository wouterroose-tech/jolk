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
    private final boolean isMethod;

    public JolkRootNode(JolkLanguage language, JolkNode bodyNode, String name, boolean isMethod) {
        super(language);
        this.bodyNode = bodyNode;
        this.name = name;
        this.isMethod = isMethod;
    }

    public JolkRootNode(JolkLanguage language, JolkNode bodyNode, String name) {
        this(language, bodyNode, name, true);
    }

    public JolkRootNode(JolkLanguage language, JolkNode bodyNode) {
        this(language, bodyNode, "root", true);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (isMethod) {
            try {
                return bodyNode.executeGeneric(frame);
            } catch (JolkReturnException e) {
                // In Jolk, the return target is the arguments array (lexical environment)
                // of the 'Home' method. We verify if this activation's environment
                // matches the target stored in the exception.
                if (e.getTarget() == frame.getArguments()) {
                    return e.getResult();
                } else {
                    throw e;
                }
            }
        } else {
            return bodyNode.executeGeneric(frame);
        }
    }

    @Override
    public String getName() {
        return name;
    }
}