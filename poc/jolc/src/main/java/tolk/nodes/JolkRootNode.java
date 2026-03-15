package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import tolk.language.JolkLanguage;
import tolk.runtime.JolkNothing;

/// The root of all Jolk execution trees.
///
/// For this stage of development, it's a minimal implementation that simply returns
/// the Jolk `null` singleton, allowing the language to be initialized and evaluated
/// without a full parser.
public class JolkRootNode extends RootNode {
    
    public JolkRootNode(JolkLanguage language) {
        super(language);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return JolkNothing.NOTHING;
    }
}