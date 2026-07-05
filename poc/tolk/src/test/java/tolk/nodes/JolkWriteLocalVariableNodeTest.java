package tolk.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkWriteLocalVariableNodeTest
 *
 * Verifies the behavior of the {@link JolkWriteLocalVariableNode}, which is responsible
 * for writing values to local variable slots within a {@link VirtualFrame}.
 */
public class JolkWriteLocalVariableNodeTest {

    /**
     * Tests writing a value to a local variable in the current frame.
     */
    @Test
    void testWriteLocalToCurrentFrame() {
        // Setup a frame with a local variable slot
        FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
        builder.addSlot(FrameSlotKind.Object, "local", null);
        FrameDescriptor fd = builder.build();
        Object[] locals = {null}; // Initial value
        VirtualFrame frame = new VirtualFrameMock(fd, locals);

        // Write "newValue" to local (index 0, depth 0)
        JolkNode valueToWrite = new JolkLiteralNode("newValue");
        JolkWriteLocalVariableNode writeNode = new JolkWriteLocalVariableNode(0, 0, valueToWrite);
        Object result = writeNode.executeGeneric(frame);

        assertEquals("newValue", result, "Write node should return the written value.");
        assertEquals("newValue", frame.getArguments()[0],
                "The local variable in the current frame should be updated.");
    }

    /**
     * Tests writing a value to a local variable in an outer (lexical) frame.
     */
    @Test
    void testWriteLocalToOuterFrame() {
        // Outer frame: has "outerLocal" at index 0
        FrameDescriptor.Builder outerBuilder = FrameDescriptor.newBuilder();
        outerBuilder.addSlot(FrameSlotKind.Object, "outerLocal", null);
        FrameDescriptor outerFd = outerBuilder.build();
        Object[] outerLocals = {"initialOuterValue"};
        VirtualFrame outerFrame = new VirtualFrameMock(outerFd, outerLocals);

        // Current frame: links to outerFrame
        FrameDescriptor currentFd = FrameDescriptor.newBuilder().build();
        Object[] currentLocals = {outerFrame};
        VirtualFrame currentFrame = new VirtualFrameMock(currentFd, currentLocals, outerFrame);

        // Write "updatedOuterValue" to "outerLocal" (index 0, depth 1)
        JolkNode valueToWrite = new JolkLiteralNode("updatedOuterValue");
        JolkWriteLocalVariableNode writeNode = new JolkWriteLocalVariableNode(0, 1, valueToWrite);
        Object result = writeNode.executeGeneric(currentFrame);

        assertEquals("updatedOuterValue", result, "Write node should return the written value.");
        assertEquals("updatedOuterValue", outerFrame.getArguments()[0],
                "The local variable in the outer frame should be updated.");
    }

    /**
     * Tests that attempting to write to a non-existent local variable slot throws an exception.
     */
    @Test
    void testWriteToNonExistentLocalSlot() {
        FrameDescriptor fd = FrameDescriptor.newBuilder().build(); // No slots
        VirtualFrame frame = new VirtualFrameMock(fd, new Object[0]);

        JolkNode valueToWrite = new JolkLiteralNode("value");
        JolkWriteLocalVariableNode writeNode = new JolkWriteLocalVariableNode(0, 0, valueToWrite); // Index 0, depth 0

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> writeNode.executeGeneric(frame),
                "Should throw an exception when trying to write to a non-existent local variable slot.");
    }

    // Note: Testing immutability of parameters (arguments) would typically be handled
    // by the JolkVisitor during AST construction, preventing a JolkWriteLocalVariableNode
    // from being created for a parameter slot in the first place.
}