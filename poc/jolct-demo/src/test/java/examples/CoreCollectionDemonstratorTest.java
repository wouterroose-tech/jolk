package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CoreCollectionDemonstratorTest {

    private final CoreCollectionDemonstrator demonstrator = new CoreCollectionDemonstrator();

    @Test
    void testRunNew() {
        jolk.lang.Array<String> result = demonstrator.runNew();
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(List.of("red", "green", "blue"), result);
    }

    @Test
    void testRunLiteral() {
        jolk.lang.Array<String> result = demonstrator.runLiteral();
        assertEquals(List.of("red", "green", "blue"), result);
    }

    @Test
    void testRunAt() {
        assertEquals("green", demonstrator.runAt());
    }

    @Test
    void testRunPut() {
        jolk.lang.Array<String> result = demonstrator.runPut();
        assertEquals(List.of("red", "yellow", "blue"), result);
    }

    @Test
    void testRunSize() {
        assertEquals(3, demonstrator.runSize());
    }

    @Test
    void testRunToString() {
        assertEquals("[red, green, blue]", demonstrator.runToString());
    }

    @Test
    void testRunForEach() {
        assertEquals("redgreenblue", demonstrator.runForEach());
    }

}
