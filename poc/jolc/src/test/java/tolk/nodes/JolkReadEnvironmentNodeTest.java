package tolk.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkReadEnvironmentNodeTest
 *
 * Verifies the behavior of the {@link JolkReadEnvironmentNode}, which is used
 * to read values from lexically enclosing frames (the "environment" for closures).
 */
public class JolkReadEnvironmentNodeTest {

    /**
     * Tests reading the environment from the current frame (depth 0).
     */
    @Test
    void testReadFromCurrentEnvironment() {
        FrameDescriptor fd = FrameDescriptor.newBuilder().build();
        Object[] locals = {"currentValue"};
        VirtualFrame frame = new VirtualFrameMock(fd, locals);

        // Read current environment (depth 0)
        JolkReadEnvironmentNode readNode = new JolkReadEnvironmentNode(0);
        assertSame(locals, readNode.executeGeneric(frame),
                "The node at depth 0 should return the current environment array identity.");
    }

    /**
     * Tests reading a variable from the immediate parent frame (depth 1).
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
        // Parent frame passed at index 0
        VirtualFrame currentFrame = new VirtualFrameMock(currentFd, new Object[]{parentFrame}, parentFrame);

        // Read parent environment (depth 1 relative to the environment chain)
        JolkReadEnvironmentNode readNode = new JolkReadEnvironmentNode(1);
        assertSame(parentLocals, readNode.executeGeneric(currentFrame),
                "The node at depth 0 should return the captured parent environment array identity.");
    }

    /**
     * Tests reading a variable from a grandparent frame (depth 2).
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
        VirtualFrame parentFrame = new VirtualFrameMock(parentFd, new Object[]{grandparentFrame}, grandparentFrame);

        // Current frame (closure's frame): links to parentFrame
        FrameDescriptor currentFd = FrameDescriptor.newBuilder().build();
        VirtualFrame currentFrame = new VirtualFrameMock(currentFd, new Object[]{parentFrame}, parentFrame);

        // Read grandparent environment (depth 2 relative to the environment chain)
        JolkReadEnvironmentNode readNode = new JolkReadEnvironmentNode(2);
        assertSame(grandparentLocals, readNode.executeGeneric(currentFrame),
                "The node at depth 1 should return the grandparent environment array identity.");
    }

    /**
     * Tests that attempting to read from a non-existent environment depth throws an error.
     */
    @Test
    void testReadFromNonExistentEnvironmentDepth() {
        FrameDescriptor fd = FrameDescriptor.newBuilder().build();
        VirtualFrame frame = new VirtualFrameMock(fd, new Object[]{ "not_a_frame" });

        // Attempt to read from depth 1 when the environment chain is broken
        JolkReadEnvironmentNode readNode = new JolkReadEnvironmentNode(1);
        assertNull(readNode.executeGeneric(frame),
                "Should return null when trying to read from a non-existent environment frame.");
    }
}