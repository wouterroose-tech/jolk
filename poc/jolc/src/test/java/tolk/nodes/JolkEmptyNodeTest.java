package tolk.nodes;

import org.junit.jupiter.api.Test;
import tolk.runtime.JolkNothing;

import static org.junit.jupiter.api.Assertions.assertSame;

public class JolkEmptyNodeTest {

    @Test
    void testExecuteGeneric() {
        JolkEmptyNode node = new JolkEmptyNode();
        assertSame(JolkNothing.INSTANCE, node.executeGeneric(null), "Empty node should evaluate to JolkNothing");
    }
}
