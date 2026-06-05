package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkNothing;

/// # JolkReadMetaPropertyNode
///
/// Reads a property (meta field/constant) directly from a JolkMetaClass receiver.
/// This node is used as the body for synthesized getters of meta fields.
public class JolkReadMetaPropertyNode extends JolkExpressionNode {
    private final String propertyName;

    public JolkReadMetaPropertyNode(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // The receiver of a meta method is the JolkMetaClass itself (at frame.getArguments()[0]).
        Object receiver = (frame.getArguments().length > 0) ? frame.getArguments()[0] : JolkNothing.INSTANCE;

        if (receiver instanceof JolkMetaClass metaClass) {
            DynamicObjectLibrary objLib = DynamicObjectLibrary.getUncached();
            // Retrieve the property value directly from the JolkMetaClass's DynamicObject storage.
            return JolkNode.lift(objLib.getOrDefault(metaClass, propertyName, JolkNothing.INSTANCE));
        }
        return JolkNothing.INSTANCE;
    }
}