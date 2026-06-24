package demonstrators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import util.JolkTestBase;

public class CoreCollectionDemonstratorTest extends JolkTestBase {

    private Value getDemonstrator() {
        Value demonstrator = load("/demonstrators/CoreCollectionDemonstrator.jolk");
        return demonstrator.invokeMember("new");
    }

    @Test
    void testRunArrayNew() {
        Value result = getDemonstrator().invokeMember("runArrayNew");
        assertNotNull(result);
        assertEquals(3, result.invokeMember("size").asLong());
    }

    @Test
    void testRunArrayNewLiteral() {
        Value result = getDemonstrator().invokeMember("runArrayLiteral");
        assertNotNull(result);
        assertEquals(3, result.invokeMember("size").asLong());
    }

    @Test
    void testRunArrayAt() {
        Value result = getDemonstrator().invokeMember("runArrayAt");
        assertNotNull(result);
        assertEquals("green", result.asString());
    }

    @Test
    void testRunMapNew() {
        Value result = getDemonstrator().invokeMember("runMapNew");
        assertNotNull(result);
        assertEquals(0, result.invokeMember("size").asLong());
    }

    @Test
    void testRunMapewLiteral() {
        Value result = getDemonstrator().invokeMember("runMapLiteral");
        assertNotNull(result);
        assertEquals(3, result.invokeMember("size").asLong());
    }

    @Test
    void testRunArrayPut() {
        @SuppressWarnings("rawtypes")
        ArrayList result = (ArrayList) getDemonstrator().invokeMember("runArrayPut").asHostObject();
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("yellow", result.get(1).toString());
    }

    @Test
    void testRunArraySize() {
        Value result = getDemonstrator().invokeMember("runArraySize");
        assertNotNull(result);
        assertEquals(3L, result.asLong());
    }

    @Test
    void testRunArrayToString() {
        Value result = getDemonstrator().invokeMember("runArrayToString");
        assertNotNull(result);
        assertEquals("[red, green, blue]", result.asString());
    }

    @Test
    void testRunArrayReduce() {
        Value result = getDemonstrator().invokeMember("runArrayReduce");
        assertNotNull(result);
        assertEquals("redgreenblue", result.asString());
    }

    @Test
    void testRunArrayForEach() {
        Value result = getDemonstrator().invokeMember("runArrayForEach");
        assertNotNull(result);
        assertEquals("redgreenblue", result.asString());
    }

    @Test
    @Disabled("Map not yet supported in Jolk")
    void testRunMapMap() {
        Value result = getDemonstrator().invokeMember("runMapMap");
        assertNotNull(result);
        // map produces Array of "key:value" strings
        assertEquals(3, result.invokeMember("size").asLong());
        String first = result.invokeMember("get", 0).asString();
        assertNotNull(first);
        // one of the entries should be "red:#FF0000"
        assertTrue(first.contains("red:") || result.invokeMember("contains", "red:#FF0000").asBoolean());
    }

    @Test
    void testRunMapForEach() {
        Value result = getDemonstrator().invokeMember("runMapForEach");
        assertNotNull(result);
        // concatenation of key+value for three entries
        assertEquals("red#FF0000green#00FF00blue#0000FF", result.asString());
    }
}
