package tolk.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;

/**
 * ## JolkReadEnvironmentNodeTest
 *
 * Verifies the behavior of the {@link JolkReadEnvironmentNode}, which is used
 * to read values from lexically enclosing frames (the "environment" for closures).
 */
@Disabled
public class JolkReadEnvironmentNodeTest {

    /**
     * Tests reading a variable from the immediate parent frame (depth 0).
     */
    @Test
    void testReadFromImmediateParentEnvironment() {
        // Parent frame: has "parentVar" at index 0
        FrameDescriptor.Builder parentBuilder = FrameDescriptor.newBuilder();
        parentBuilder.addSlot(FrameSlotKind.Object, "parentVar", null);
        FrameDescriptor parentFd = parentBuilder.build();
        Object[] parentLocals = {"parentValue"};
        VirtualFrame parentFrame = new VirtualFrameMock(parentFd, parentLocals);

        // Current frame (closure's frame): links to parentFrame
        FrameDescriptor currentFd = FrameDescriptor.newBuilder().build();
        // Parent data passed at index 0
        VirtualFrame currentFrame = new VirtualFrameMock(currentFd, new Object[]{parentLocals}, parentFrame);

        // Read "parentVar" (index 0, depth 0 relative to the environment chain)
        JolkReadEnvironmentNode readNode = new JolkReadEnvironmentNode(0);
        assertSame(parentLocals, readNode.executeGeneric(currentFrame),
                "The node at depth 0 should return the captured parent environment array identity.");
    }

    /**
     * Tests reading a variable from a grandparent frame (depth 1).
     * This verifies "Deep Lexical Capture".
     */
    @Test
    void testReadFromGrandparentEnvironment() {
        // Grandparent frame: has "grandparentVar" at index 0
        FrameDescriptor.Builder grandparentBuilder = FrameDescriptor.newBuilder();
        grandparentBuilder.addSlot(FrameSlotKind.Object, "grandparentVar", null);
        FrameDescriptor grandparentFd = grandparentBuilder.build();
        Object[] grandparentLocals = {"grandparentValue"};
        VirtualFrame grandparentFrame = new VirtualFrameMock(grandparentFd, grandparentLocals);

        // Parent frame: links to grandparentFrame
        FrameDescriptor parentFd = FrameDescriptor.newBuilder().build();
        VirtualFrame parentFrame = new VirtualFrameMock(parentFd, new Object[]{grandparentLocals}, grandparentFrame);

        // Current frame (closure's frame): links to parentFrame
        FrameDescriptor currentFd = FrameDescriptor.newBuilder().build();
        Object[] parentArgs = parentFrame.getArguments();
        VirtualFrame currentFrame = new VirtualFrameMock(currentFd, new Object[]{parentArgs}, parentFrame);

        // Read "grandparentVar" (index 0, depth 1 relative to the environment chain)
        JolkReadEnvironmentNode readNode = new JolkReadEnvironmentNode(1);
        assertSame(grandparentLocals, readNode.executeGeneric(currentFrame),
                "The node at depth 1 should return the grandparent environment array identity.");
    }

    /**
     * Tests that attempting to read from a non-existent environment depth throws an error.
     */
    @Test
    void testReadFromNonExistentEnvironmentDepth() {
        FrameDescriptor fd = FrameDescriptor.newBuilder().build();
        VirtualFrame frame = new VirtualFrameMock(fd, new Object[0]);

        // Attempt to read from depth 0 when there is no parent frame
        JolkReadEnvironmentNode readNode = new JolkReadEnvironmentNode(0);
        assertThrows(ClassCastException.class, () -> readNode.executeGeneric(frame),
                "Should throw an exception when trying to read from a non-existent environment frame.");
    }
}