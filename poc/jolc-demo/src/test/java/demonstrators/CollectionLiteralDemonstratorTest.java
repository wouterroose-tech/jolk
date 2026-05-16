package demonstrators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import util.JolkTestBase;

public class CollectionLiteralDemonstratorTest  extends JolkTestBase {

    private Value getDemonstrator() {
        Value demonstrator = getJolkClass("/demonstrators/CollectionLiteralDemonstrator.jolk");
        return demonstrator.invokeMember("new");
    }
    
    @Test
    void testRunArrayLiteral() {
        Value result = getDemonstrator().invokeMember("runArrayLiteral");
        assertEquals(List.of("a", "b", "c"), List.of(result.as(String[].class)));
    }

    @Test
    void testRunArrayLiteralField() {
        Value result = getDemonstrator().invokeMember("runArrayLiteralField");
        assertEquals(List.of("red", "green", "blue"), List.of(result.as(String[].class)));
    }

    @Test
    void testRunEmptyArray() {
        Value result = getDemonstrator().invokeMember("runEmptyArray");
        assertEquals(List.of(), result.as(List.class));
    }

    @Test
    void testRunContains() {
        Value demonstrator = getDemonstrator();
        assertTrue(demonstrator.invokeMember("runContains", "red").asBoolean());
        assertFalse(demonstrator.invokeMember("runContains", "yellow").asBoolean());
    }

    @Test
    void testRunNestedArray() {
        Value result = getDemonstrator().invokeMember("runNestedArray");
        Long[][] nested = result.as(Long[][].class);
        assertEquals(2, nested.length);
        assertEquals(List.of(1L, 2L), List.of(nested[0]));
        assertEquals(List.of(3L, 4L), List.of(nested[1]));
    }

}
