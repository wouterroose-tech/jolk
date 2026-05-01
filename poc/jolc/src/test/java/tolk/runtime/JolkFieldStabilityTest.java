package tolk.runtime;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkFieldStabilityTest
 *
 * Verifies the enforcement of the 'Lexical Fence' for stable and constant fields.
 * These tests ensure that the runtime correctly prevents mutation of identities
 * marked for instance-level or meta-level stability.
 */
public class JolkFieldStabilityTest extends JolcTestBase {

    @Test
    void testStableFieldImmutability() {
        // In Jolk, 'stable' fields should not generate a setter or allow mutation.
        String source = """
            class StableTest {
                public stable Long id;
            }
            """;
        Value meta = eval(source);
        Value instance = meta.invokeMember("new", 42L);

        // Getter should work
        assertEquals(42L, instance.invokeMember("id").asLong());

        // Attempting to mutate a stable field should fail. 
        // Depending on implementation, it either throws or simply doesn't exist as an invocable setter.
        assertThrows(Exception.class, () -> instance.invokeMember("id", 99L));
    }

    @Test
    void testMetaConstantImmutability() {
        String source = """
            class ConstTest {
                public meta constant Long VERSION = 1;
            }
            """;
        Value meta = eval(source);

        // Accessing the constant
        assertEquals(1L, meta.invokeMember("VERSION").asLong());

        // Constants are non-assignable post-initialization
        assertThrows(Exception.class, () -> meta.invokeMember("VERSION", 2L) );
    }

    @Test
    void testRecordImmutabilityEnforcement() {
        // Records are inherently stable. The JolkObject kernel blocks setters for RECORD archetypes.
        String source = """
            record Point {
                Long x;
                Long y;
            }
            """;
        Value meta = eval(source);
        Value point = meta.invokeMember("new", 10L, 20L);

        assertEquals(10L, point.invokeMember("x").asLong());

        // Records must not have setters
        assertThrows(Exception.class, () -> point.invokeMember("x", 100L) );
    }

    @Test
    void testLazyMetaFieldInitialization() {
        String source = """
            class LazyMetaFieldTest {
                meta Long count = 0;
                meta Long initialize() {
                    self #count( self #count + 1);
                    ^ 100;
                }
                meta lazy Long VALUE = self #initialize;
            }
            """;
        Value meta = eval(source);
        assertEquals(0, meta.invokeMember("count").asLong());
        assertEquals(100L, meta.invokeMember("VALUE").asLong());
        assertEquals(1, meta.invokeMember("count").asLong());
        assertEquals(100L, meta.invokeMember("VALUE").asLong());
        assertEquals(1, meta.invokeMember("count").asLong());
    }
}