package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkStringTest extends JolcTestBase {

    @Test
    void testStringField() {
        String source = "class Container { String val; }";
        Value meta = eval(source);

        Value instance = meta.invokeMember("new");
        assertFalse(instance.invokeMember("val").isNull(), "String fields should default to empty string.");
        assertTrue(instance.invokeMember("val").asString().isEmpty());

        instance.invokeMember("val", "hello");
        assertEquals("hello", instance.invokeMember("val").asString());
        instance.invokeMember("val", "world");
        assertEquals("world", instance.invokeMember("val").asString());

        // Canonical #new
        instance = meta.invokeMember("new", "test");
        assertEquals("test", instance.invokeMember("val").asString(), "Canonical #new should initialize String fields.");
    }
    
    @Test
    void testEquivalence() {
        String source = """
            class EqualityTest {
                Boolean eq(String a, String b) { ^ a ~~ b }
                Boolean ne(String a, String b) { ^ a !~ b }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("eq", "hello", "hello").asBoolean());
        assertFalse(instance.invokeMember("eq", "hello", "world").asBoolean());
        assertTrue(instance.invokeMember("ne", "hello", "world").asBoolean());
        assertFalse(instance.invokeMember("ne", "hello", "hello").asBoolean());
    }
    
    @Test
    void testMatches() {
        String source = """
            class RegexTest {
                Boolean matches(String str, String pattern) { ^ str #matches(pattern) }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("matches", "hello123", "hello\\d+").asBoolean());
        assertFalse(instance.invokeMember("matches", "hello", "hello\\d+").asBoolean());
    }
    
    @Test
    void testConcatenation() {
        String source = """
            class ConcatTest {
                String concat(String a, String b) { ^ a + b }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertEquals("hello", instance.invokeMember("concat", "hello", "").asString());
        assertEquals("world", instance.invokeMember("concat", "", "world").asString());
        assertEquals("helloworld", instance.invokeMember("concat", "hello", "world").asString());
    }
    
    @Test
    void testJavaProtocol() {
        String source = """
            class ConcatTest {
                Long length(String str) { ^ str #length() }
                Boolean contains(String a, String b) { ^ a #contains(b) }
                String toUpperCase(String str) { ^ str #toUpperCase() }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertEquals(5L, instance.invokeMember("length", "hello").asLong());
        assertEquals(0L, instance.invokeMember("length", "").asLong());
        assertTrue(instance.invokeMember("contains", "hello", "ell").asBoolean());
        assertFalse(instance.invokeMember("contains", "hello", "world").asBoolean());
        assertEquals("HELLO", instance.invokeMember("toUpperCase", "hello").asString());
    }
}
