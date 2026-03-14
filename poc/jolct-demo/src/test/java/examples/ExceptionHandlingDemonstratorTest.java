package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ExceptionHandlingDemonstratorTest {

    private final ExceptionHandlingDemonstrator demonstrator = new ExceptionHandlingDemonstrator();

    @Test
    void testRunCatch() {
        assertEquals("Caught: Boom", demonstrator.runCatch());
    }

    @Test
    void testRunMultiCatch() {
        assertEquals("Caught First", demonstrator.runMultiCatch(true));
        assertEquals("Caught Second", demonstrator.runMultiCatch(false));
    }

    @Test
    void testRunFinally() {
        assertEquals("Start -> Catch -> Finally", demonstrator.runFinally());
    }

}
