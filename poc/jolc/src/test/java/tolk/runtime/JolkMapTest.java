package tolk.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

/// # JolkMapTest
/// 
/// Validates the runtime implementation of the Map archetype extensions.
/// This includes map literal synthesis, field initialization, and 
/// behavioral verification of the messaging protocol (#size, #at, #put, etc.).
public class JolkMapTest extends JolcTestBase {

    @Test
    void testEmptyMap() {
        /// Verifies the creation of an empty map literal.
        String source = """ 
            class MyClass {
                HashMap<Object, Object> emptyMap = HashMap #new;
                Long size() { ^ self #emptyMap #size }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(0L, instance.invokeMember("size").asLong());
    }

    @Test
    void testEmptyMapLiteral() {
        /// Verifies the creation of an empty map literal.
        String source = """ 
            class MyClass {
                HashMap<Object, Object> emptyMap = #();
                Long size() { ^ self #emptyMap #size }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(0L, instance.invokeMember("size").asLong());
    }

    @Test
    void testMapLiteralField() {
        /// Verifies that a map literal can be used to initialize a field.
        String source = """ 
            class MyClass {
                stable HashMap<String, Long> ages = #("Alice" -> 30, "Bob" -> 25);
                Long getAge(String name) { ^ self #ages #at(name) }
            }""";
        Value instance = eval(source).invokeMember("new");
        Value ages = instance.invokeMember("ages");
        assertEquals(2L, ages.invokeMember("size").asLong()); // size() exists on java.util.Map
        assertEquals(30L, instance.invokeMember("getAge", "Alice").asLong());
        assertEquals(25L, instance.invokeMember("getAge", "Bob").asLong());
    }

    @Test
    void testAtMethod() {
        /// Verifies the #at(key) method for retrieving values.
        String source = """ 
            class MyClass {
                Map<String, Long> ages = #("Alice" -> 30, "Bob" -> 25);
                Long getAge(String name) { ^ self #ages #at(name) }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(30L, instance.invokeMember("getAge", "Alice").asLong());
        assertEquals("null", instance.invokeMember("getAge", "Charlie").toString(), "Should return JolkNothing");
    }

    @Test
    void testPutMethod() {
        /// Verifies the #put(key, value) method for adding/updating entries.
        String source = """ 
            class MyClass {
                Map<String, Long> ages = #("Alice" -> 30);
                Map<String, Long> setAge(String name, Long age) { ^ self #ages #put(name, age) }
                Long getAge(String name) { ^ self #ages #at(name) }
            }""";
        Value instance = eval(source).invokeMember("new");
        instance.invokeMember("setAge", "Alice", 40L);
        assertEquals(40L, instance.invokeMember("getAge", "Alice").asLong());

        instance.invokeMember("setAge", "Bob", 20L);
        assertEquals(40L, instance.invokeMember("getAge", "Alice").asLong());
        assertEquals(20L, instance.invokeMember("getAge", "Bob").asLong());
    }

    @Test
    void testContainsKeyMethod() {
        /// Verifies the #containsKey(key) method.
        String source = """ 
            class MyClass {
                Map<String, Long> ages = #("Alice" -> 30);
                Boolean hasKey(String name) { ^ self #ages #containsKey(name) }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("hasKey", "Alice").asBoolean());
        assertFalse(instance.invokeMember("hasKey", "Charlie").asBoolean());
    }

    @Test
    void testForEachMethod() {
        /// Verifies the #forEach(closure) method for iterating over entries.
        String source = """ 
            class MyClass {
                Map<String, Long> ages = #("Alice" -> 30, "Bob" -> 25);
                String collectEntries() {
                    String result = "";
                    self #ages #forEach [ k, v -> result = result + k + ":" + v #toString + ";" ];
                    ^ result
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        // Note: Map iteration order is guaranteed for LinkedHashMap, which Jolk uses.
        assertEquals("Alice:30;Bob:25;", instance.invokeMember("collectEntries").asString());
    }

    @Test
    void testMapWithLongKeys() {
        /// Verifies that maps correctly handle Long keys and retrieval.
        String source = """
            class MapLongTest {
                Map<Long, String> names = #(1 -> "one", 2 -> "two");
                String getName(Long id) { ^ self #names #at(id) }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("one", instance.invokeMember("getName", 1L).asString());
        assertEquals("two", instance.invokeMember("getName", 2L).asString());
        assertEquals("null", instance.invokeMember("getName", 3L).toString());
    }

    @Test
    void testNestedMap() {
        /// Verifies that maps can be nested and accessed.
        String source = """
            class NestedMapTest {
                Map<String, Map<String, Long>> data = #( "outer" -> #( "inner" -> 42 ) );
                Long getInner() { ^ self #data #at("outer") #at("inner") }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(42L, instance.invokeMember("getInner").asLong());
    }
}
