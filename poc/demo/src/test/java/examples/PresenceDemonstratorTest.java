package examples;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PresenceDemonstratorTest {

    private final PresenceDemonstrator demo = new PresenceDemonstrator();
    private final jolk.lang.Object presentObject = new jolk.lang.Object();
    private final jolk.lang.Object emptyObject = null;

    @Test
    public void testRunIsPresent() {
        assertTrue(demo.runIsPresent(presentObject));
        assertFalse(demo.runIsPresent(emptyObject));
    }

    @Test
    public void testRunIsEmpty() {
        assertFalse(demo.runIsEmpty(presentObject));
        assertTrue(demo.runIsEmpty(emptyObject));
    }

    @Test
    public void testRunIfPresent() {
        assertEquals("Present", demo.runIfPresent(presentObject));
        assertEquals("Empty", demo.runIfPresent(emptyObject));
    }

    @Test
    public void testRunIfEmpty() {
        assertEquals("Present", demo.runIfEmpty(presentObject));
        assertEquals("Empty", demo.runIfEmpty(emptyObject));
    }
}