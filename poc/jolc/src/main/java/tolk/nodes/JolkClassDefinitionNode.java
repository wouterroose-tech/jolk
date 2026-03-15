package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import tolk.runtime.JolkType;

/// An AST node that represents the definition of a Jolk class. When executed,
/// it produces a [JolkType] meta-object.
/// 
@NodeInfo(language = "Jolk", description = "The node for defining a Jolk class.")
public class JolkClassDefinitionNode extends JolkExpressionNode {

    private final String className;

    public JolkClassDefinitionNode(String className) {
        this.className = className;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // In a full implementation, this would also register the type in the language context.
        return new JolkType(className);
    }
}