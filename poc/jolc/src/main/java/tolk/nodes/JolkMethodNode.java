package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/// # JolkMethodNode
///
/// Represents a method definition in the Jolk AST.
/// It encapsulates the executable body and parameter metadata.
public class JolkMethodNode extends JolkNode {

    private final String name;
    @Child private JolkNode body;
    private final String[] parameters;
    private final boolean isVariadic;
    private final int frameSlots;
    private final boolean hasNL;

    public JolkMethodNode(String name, JolkNode body, String[] parameters, boolean isVariadic, int frameSlots, boolean hasNL) {
        this.name = name;
        this.body = body;
        this.parameters = parameters;
        this.isVariadic = isVariadic;
        this.frameSlots = frameSlots;
        this.hasNL = hasNL;
    }

    /**
     * Convenience constructor for cases where frameSlots is not explicitly provided,
     * defaulting to 0.
     */
    public JolkMethodNode(String name, JolkNode body, String[] parameters, boolean isVariadic) {
        this(name, body, parameters, isVariadic, 0, true);
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
}
