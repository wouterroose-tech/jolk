package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import tolk.runtime.JolkVisibility;

/// # JolkMethodNode
///
/// Represents a method definition in the Jolk AST.
/// It encapsulates the executable body and parameter metadata.
public class JolkMethodNode extends JolkNode {

    private final String name;
    private final String[] parameters;
    @Child private JolkNode body;
    private final boolean isVariadic;
    private final int frameSlots;
    private final boolean hasNL;
    private JolkVisibility visibility = JolkVisibility.PUBLIC;
    private final boolean isSignatureOnly;

    public JolkMethodNode(String name, JolkNode body, String[] parameterNames, boolean isVariadic, int frameSlots, boolean hasNL, boolean isLazy, boolean isSignatureOnly) {
        this.name = name;
        this.body = body; // The body of the method
        this.parameters = parameterNames; // Store the parameter names directly
        this.isVariadic = isVariadic;
        this.frameSlots = frameSlots;
        this.hasNL = hasNL;
        this.isSignatureOnly = isSignatureOnly;
        // unUse the isLazy parameter for now, as we are not implementing lazy evaluation in this version.
        // this.isLazy = isLazy;
    }

    public JolkMethodNode(String name, JolkNode body, String[] parameters, boolean isVariadic, int frameSlots, boolean hasNL) {
        this(name, body, parameters, isVariadic, frameSlots, hasNL, false, false);
    }

    /**
     * Convenience constructor for cases where frameSlots is not explicitly provided,
     * defaulting to 0.
     */
    public JolkMethodNode(String name, JolkNode body, String[] parameters, boolean isVariadic) {
        this(name, body, parameters, isVariadic, 0, true, false, false);
    }

    public String getName() {
        return name;
    }

    public JolkNode getBody() {
        return body;
    }

    public int getFrameSlots() {
        return frameSlots;
    }

    public boolean isSignatureOnly() {
        return isSignatureOnly;
    }

    /**
     * Returns true if this method contains a non-local return terminal (^).
     */
    public boolean hasNL() {
        return hasNL;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return body.executeGeneric(frame);
    }

    public String[] getParameters() {
        return parameters;
    }

    public boolean isVariadic() {
        return isVariadic;
    }

    public void setVisibility(JolkVisibility visibility) {
        this.visibility = visibility;
    }

    public JolkVisibility getVisibility() {
        return visibility;
    }
}
