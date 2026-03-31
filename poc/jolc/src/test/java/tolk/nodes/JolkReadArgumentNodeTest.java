package tolk.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkReadArgumentNodeTest
 *
 * Verifies the behavior of the {@link JolkReadArgumentNode}, which is responsible
 * for reading values from the argument slots of a {@link VirtualFrame}.
 */
public class JolkReadArgumentNodeTest {

    /**
     * Tests reading an argument from the current frame.
     */
    @Test
    void testReadArgumentFromCurrentFrame() {
        // Setup a frame with arguments
        FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
        builder.addSlot(FrameSlotKind.Object, "arg0", null);
        builder.addSlot(FrameSlotKind.Object, "arg1", null);
        FrameDescriptor fd = builder.build();
        Object[] arguments = {"value0", 123L};
        VirtualFrame frame = new VirtualFrameMock(fd, arguments);

        // Read arg0 (index 0, depth 0)
        JolkReadArgumentNode readArg0 = new JolkReadArgumentNode(0, 0);
        assertEquals("value0", readArg0.executeGeneric(frame), "Should read the correct argument from the current frame.");

        // Read arg1 (index 1, depth 0)
        JolkReadArgumentNode readArg1 = new JolkReadArgumentNode(1, 0);
        assertEquals(123L, readArg1.executeGeneric(frame), "Should read the correct argument from the current frame.");
    }

    /**
     * Tests reading an argument from an outer (lexical) frame.
     */
    @Test
    void testReadArgumentFromOuterFrame() {
        // Outer frame: has "outerArg" at index 0
        FrameDescriptor.Builder outerBuilder = FrameDescriptor.newBuilder();
        outerBuilder.addSlot(FrameSlotKind.Object, "outerArg", null);
        FrameDescriptor outerFd = outerBuilder.build();
        Object[] outerArgs = {"outerValue"};
        VirtualFrame outerFrame = new VirtualFrameMock(outerFd, outerArgs);

        // Current frame: has "currentArg" at index 0, links to outerFrame
        FrameDescriptor.Builder currentBuilder = FrameDescriptor.newBuilder();
        currentBuilder.addSlot(FrameSlotKind.Object, "currentArg", null);
        FrameDescriptor currentFd = currentBuilder.build();
        // Jolk environment chain: arguments[0] holds the outer context.
        Object[] currentArgs = {outerFrame, "currentValue"};
        VirtualFrame currentFrame = new VirtualFrameMock(currentFd, currentArgs, outerFrame);

        // Read "outerArg" (index 0, depth 1)
        JolkReadArgumentNode readOuterArg = new JolkReadArgumentNode(0, 1);
        assertEquals("outerValue", readOuterArg.executeGeneric(currentFrame),
                "Should read the correct argument from the outer frame.");
    }
}