package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ExpressionDemonstratorTest {

    private final ExpressionDemonstrator demonstrator = new ExpressionDemonstrator();

    @Test
    void testRunNullCoalescing() {
        assertEquals("Value", demonstrator.runNullCoalescing("Value"));
        assertEquals("Default", demonstrator.runNullCoalescing(null));
    }

    @Test
    void testRunNullCoalescingChain() {
        assertEquals("Value1", demonstrator.runNullCoalescingChain("Value1", "Value2"));
        assertEquals("Value2", demonstrator.runNullCoalescingChain(null, "Value2"));
        assertEquals("Fallback", demonstrator.runNullCoalescingChain(null, null));
    }
}
