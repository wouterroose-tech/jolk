package tolk.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * A simple mock implementation of {@link VirtualFrame} for testing purposes.
 * It allows setting up arguments and local variables, and linking to a parent frame.
 */
public class VirtualFrameMock implements MaterializedFrame {
    private final FrameDescriptor frameDescriptor;
    private final Object[] arguments;
    private final Object[] locals;
    private final VirtualFrame parentFrame;

    public VirtualFrameMock(FrameDescriptor frameDescriptor, Object[] arguments) {
        this(frameDescriptor, arguments, null);
    }

    public VirtualFrameMock(FrameDescriptor frameDescriptor, Object[] locals, VirtualFrame parentFrame) {
        this.frameDescriptor = frameDescriptor;
        this.arguments = locals; // In this mock, arguments and locals are the same array for simplicity
        this.locals = locals;
        this.parentFrame = parentFrame;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public Object getObject(int slotIndex) {
        return locals[slotIndex];
    }

    @Override
    public Object getObject(FrameSlot slot) {
        return FrameUtil.getObject(this, slot);
    }

    @Override
    public void setObject(int slotIndex, Object value) {
        locals[slotIndex] = value;
    }

    @Override
    public void setObject(FrameSlot slot, Object value) {
        FrameUtil.setObject(this, slot, value);
    }

    @Override
    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }

    @Override
    public MaterializedFrame materialize() {
        return this;
    }

    @Override
    public VirtualFrame getParentFrame() {
        return parentFrame;
    }
}