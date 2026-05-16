package tolk.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JolkMatchTest {

    @Test
    void testWith() {
        String value = "test";
        JolkMatch match = JolkMatch.with(value);
        assertTrue(match.isPresent(), "Match should be present");
        assertEquals(value, match.getValue().toString(), "Match value should match");
    }

    @Test
    void testEmpty() {
        JolkMatch match = JolkMatch.empty();
        assertFalse(match.isPresent(), "Empty match should not be present");
        assertEquals(JolkNothing.INSTANCE, match.getValue(), "Empty match value should be Nothing instance");
    }

    @Test
    void testToDisplayString() {
        JolkMatch match = JolkMatch.with("test");
        assertEquals("Match(test)", match.toDisplayString(false));
        
        JolkMatch empty = JolkMatch.empty();
        assertEquals("Match.empty", empty.toDisplayString(false));
    }

    @Test
    void testGet() {
        JolkMatch match = JolkMatch.with("test");
        assertEquals("test", match.getValue().toString(), "get should return the value when present");
        
        JolkMatch empty = JolkMatch.empty();
        assertEquals(JolkNothing.INSTANCE, empty.getValue(), "get should return JolkNothing when empty");
    }
}
