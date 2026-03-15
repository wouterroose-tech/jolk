package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkVisibility;

/// An AST node that represents the definition of a Jolk class. When executed,
/// it produces a [JolkMetaClass] meta-object.
/// 
@NodeInfo(language = "Jolk", description = "The node for defining a Jolk class.")
public class JolkClassDefinitionNode extends JolkExpressionNode {

    private final String className;
    private final JolkFinality finality;
    private final JolkVisibility visibility;

    public JolkClassDefinitionNode(String className, JolkFinality finality, JolkVisibility visibility) {
        this.className = className;
        this.finality = finality;
        this.visibility = visibility;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // In a full implementation, this would also register the type in the language context.
        return new JolkMetaClass(className, finality, visibility);
    }
}