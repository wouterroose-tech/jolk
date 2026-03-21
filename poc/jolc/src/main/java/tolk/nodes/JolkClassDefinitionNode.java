package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import tolk.language.JolkLanguage;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkVisibility;
import tolk.runtime.JolkArchetype;
import tolk.runtime.JolkClosure;

/// An AST node that represents the definition of a Jolk type.
///
/// This node captures the core structural properties of a type—such as its name,
/// archetype, visibility, and finality—during parsing. When executed at runtime, it
/// acts as a factory, instantiating a [JolkMetaClass] meta-object with these
/// properties. This meta-object then becomes the first-class representation of the
/// type within the Jolk interpreter.
/// 
@NodeInfo(language = "Jolk", description = "The node for defining a Jolk class.")
public class JolkClassDefinitionNode extends JolkExpressionNode {

    private final String className;
    private final JolkFinality finality;
    private final JolkVisibility visibility;
    private final JolkArchetype archetype;
    private final Map<String, Object> instanceMembers;
    private final Map<String, Object> instanceFields;

    public JolkClassDefinitionNode(String className, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> instanceFields) {
        this.className = className;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceMembers = instanceMembers;
        this.instanceFields = instanceFields;
    }

    public JolkClassDefinitionNode(String className, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype) {
        this(className, finality, visibility, archetype, Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        JolkLanguage language = null;
        if (getRootNode() != null) {
            language = getRootNode().getLanguage(JolkLanguage.class);
        }
        Map<String, Object> runtimeMembers = new LinkedHashMap<>();
        
        for (Map.Entry<String, Object> entry : instanceMembers.entrySet()) {
            if (entry.getValue() instanceof JolkMemberNode member) {
                JolkRootNode root = new JolkRootNode(language, member.getBody(), member.getName());
                runtimeMembers.put(entry.getKey(), new JolkClosure(root.getCallTarget()));
            } else {
                runtimeMembers.put(entry.getKey(), entry.getValue());
            }
        }

        return new JolkMetaClass(className, null, finality, visibility, archetype, runtimeMembers, instanceFields, Collections.emptyMap());
    }
}