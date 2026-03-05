package examples;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TernaryExpressionDemonstratorTest {

    private final TernaryExpressionDemonstrator demo = new TernaryExpressionDemonstrator();

    @Test
    public void testRunReturn() {
        assertEquals("True", demo.runReturn(true));
        assertEquals("False", demo.runReturn(false));
    }

    @Test
    public void testRunAssignment() {
        assertEquals("A", demo.runAssignment(true));
        assertEquals("B", demo.runAssignment(false));
    }

    @Test
    public void testRunChain() {
        assertEquals("High", demo.runChain(11));
        assertEquals("Medium", demo.runChain(6));
        assertEquals("Low", demo.runChain(5));
    }

    @Test
    public void testRunOneBranch() {
        assertEquals("Hit", demo.runOneBranch(true));
        assertEquals("Miss", demo.runOneBranch(false));
    }

    @Test
    public void testRunAssignmentIfNot() {
        assertEquals("B", demo.runAssignmentIfNot(true));
        assertEquals("A", demo.runAssignmentIfNot(false));
    }

    @Test
    public void testRunIfNotThen() {
        assertEquals(true, demo.runIfNotThen(true));
        assertEquals(false, demo.runIfNotThen(false));
    }

    @Test
    public void testRunIfNotThenElse() {
        assertEquals(true, demo.runIfNotThenElse(true));
        assertEquals(false, demo.runIfNotThenElse(false));
    }

    @Test
    public void testRunBlockAssignment() {
        assertEquals(1, demo.runBlockAssignment(true));
        assertEquals(2, demo.runBlockAssignment(false));
    }
}