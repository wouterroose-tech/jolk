package demonstrators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import util.JolkTestBase;

public class CoreCollectionDemonstratorTest extends JolkTestBase {

    private Value getDemonstrator() {
        Value demonstrator = getJolkClass("/demonstrators/CoreCollectionDemonstrator.jolk");
        return demonstrator.invokeMember("new");
    }

    @Test
    void testRunArrayNew() {
        Value result = getDemonstrator().invokeMember("runArrayNew");
        assertNotNull(result);
        assertEquals(3L, result.invokeMember("size").asLong());
    }

    @Test
    void testRunArrayNewLiteral() {
        Value result = getDemonstrator().invokeMember("runArrayLiteral");
        assertNotNull(result);
        assertEquals(3L, result.invokeMember("size").asLong());
    }

    @Test
    void testRunArrayAt() {
        Value result = getDemonstrator().invokeMember("runArrayAt");
        assertNotNull(result);
        assertEquals("green", result.asString());
    }
}
