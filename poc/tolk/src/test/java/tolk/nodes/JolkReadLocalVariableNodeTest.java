package tolk.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkReadLocalVariableNodeTest
 *
 * Verifies the behavior of the {@link JolkReadLocalVariableNode}, which is responsible
 * for reading values from local variable slots within a {@link VirtualFrame}.
 */
public class JolkReadLocalVariableNodeTest {

    /**
     * Tests reading a local variable from the current frame.
     */
    @Test
    void testReadLocalFromCurrentFrame() {
        // Setup a frame with local variables
        FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
        builder.addSlot(FrameSlotKind.Object, "local0", null);
        builder.addSlot(FrameSlotKind.Object, "local1", null);
        FrameDescriptor fd = builder.build();
        Object[] locals = {"value0", 42L};
        VirtualFrame frame = new VirtualFrameMock(fd, locals);

        // Read local0 (index 0, depth 0)
        JolkReadLocalVariableNode readLocal0 = new JolkReadLocalVariableNode(0, 0);
        assertEquals("value0", readLocal0.executeGeneric(frame), "Should read the correct local variable from the current frame.");

        // Read local1 (index 1, depth 0)
        JolkReadLocalVariableNode readLocal1 = new JolkReadLocalVariableNode(1, 0);
        assertEquals(42L, readLocal1.executeGeneric(frame), "Should read the correct local variable from the current frame.");
    }

    /**
     * Tests reading a local variable from an outer (lexical) frame.
     */
    @Test
    void testReadLocalFromOuterFrame() {
        // Outer frame: has "outerLocal" at index 0
        FrameDescriptor.Builder outerBuilder = FrameDescriptor.newBuilder();
        outerBuilder.addSlot(FrameSlotKind.Object, "outerLocal", null);
        FrameDescriptor outerFd = outerBuilder.build();
        Object[] outerLocals = {"outerValue"};
        VirtualFrame outerFrame = new VirtualFrameMock(outerFd, outerLocals);

        // Current frame: has "currentLocal" at index 0, links to outerFrame
        FrameDescriptor.Builder currentBuilder = FrameDescriptor.newBuilder();
        currentBuilder.addSlot(FrameSlotKind.Object, "currentLocal", null);
        FrameDescriptor currentFd = currentBuilder.build();
        // Environment chain at index 0
        Object[] currentLocals = {outerFrame, "currentValue"};
        VirtualFrame currentFrame = new VirtualFrameMock(currentFd, currentLocals, outerFrame);

        // Read "outerLocal" (index 0, depth 1)
        JolkReadLocalVariableNode readOuterLocal = new JolkReadLocalVariableNode(0, 1);
        assertEquals("outerValue", readOuterLocal.executeGeneric(currentFrame),
                "Should read the correct local variable from the outer frame.");
    }
}