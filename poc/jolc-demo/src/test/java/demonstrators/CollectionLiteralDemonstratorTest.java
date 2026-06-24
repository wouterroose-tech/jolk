package demonstrators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.stream.Collectors;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import util.JolkTestBase;

public class CollectionLiteralDemonstratorTest extends JolkTestBase {

    private Value getDemonstrator() {
        Value demonstrator = load("/demonstrators/CollectionLiteralDemonstrator.jolk");
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
    void testRunArrayContains() {
        Value demonstrator = getDemonstrator();
        assertTrue(demonstrator.invokeMember("runArrayContains", "red").asBoolean());
        assertFalse(demonstrator.invokeMember("runArrayContains", "yellow").asBoolean());
    }

    @Test
    void testRunNestedArray() {
        Value result = getDemonstrator().invokeMember("runNestedArray");
        Long[][] nested = result.as(Long[][].class);
        assertEquals(2, nested.length);
        assertEquals(List.of(1L, 2L), List.of(nested[0]));
        assertEquals(List.of(3L, 4L), List.of(nested[1]));
    }
        
    @Test
    void testRunMapLiteralField() {
        Value result = getDemonstrator().invokeMember("runMapLiteralField");
        assertEquals(3L, result.invokeMember("size").asLong());
        @SuppressWarnings("unchecked")
        java.util.Map<Object,Object> raw = result.as(java.util.Map.class);
        java.util.Map<String,String> map = raw.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        assertEquals("#FF0000", map.get("red"));
    }

    @Test
    void testRunMapGet() {
        Value demonstrator = getDemonstrator();
        assertEquals("#FF0000", demonstrator.invokeMember("runMapGet", "red").asString());
        assertEquals("#00FF00", demonstrator.invokeMember("runMapGet", "green").asString());
    }

    @Test
    void testRunMapContainsKey() {
        Value demonstrator = getDemonstrator();
        assertTrue(demonstrator.invokeMember("runMapContainsKey", "red").asBoolean());
        assertFalse(demonstrator.invokeMember("runMapContainsKey", "purple").asBoolean());
    }

    @Test
    void testRunMapSizeAndEmpty() {
        Value demonstrator = getDemonstrator();
        assertEquals(3L, demonstrator.invokeMember("runMapSize").asLong());
        Value empty = demonstrator.invokeMember("runEmptyMap");
        java.util.Map<?,?> emptyMap = empty.as(java.util.Map.class);
        assertEquals(0, emptyMap.size());
    }

}
