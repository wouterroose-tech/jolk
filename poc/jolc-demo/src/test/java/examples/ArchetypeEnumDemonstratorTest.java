package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.graalvm.polyglot.Value;

import util.JolkTestBase;

/// # ArchetypeEnumDemonstratorTest
///
/// Verifies the interaction with the `enum` archetype as showcased by the 
/// [ArchetypeEnumDemonstrator].
public class ArchetypeEnumDemonstratorTest extends JolkTestBase {

    private Value getDemonstrator() {
        getJolkClass("/demo/validation/engine/Level.jolk");
        Value demonstrator = getJolkClass("/examples/ArchetypeEnumDemonstrator.jolk");
        return demonstrator.invokeMember("new");
    }

    /// ### testRunAccessConstant
    ///
    /// Verifies that enum constants are accessible via meta-selectors on the Type.
    @Test
    void testRunAccessConstant() {
        Value level = getDemonstrator().invokeMember("runAccessConstant");
        assertNotNull(level);
        assertFalse(level.isNull(), "Enum constant should not be 'Nothing'.");
        assertEquals("ERROR", level.invokeMember("name").asString());
    }

    /// ### testRunName
    ///
    /// Verifies the synthesized `#name` message accessor.
    @Test
    void testRunName() {
        assertEquals("ERROR", getDemonstrator().invokeMember("runName").asString());
    }

    @Test
    void testRunVariable() {
        assertEquals("WARNING", getDemonstrator().invokeMember("runVariable").asString());
    }

    @Test
    void testRunConstant() {
        assertEquals("INFO", getDemonstrator().invokeMember("runConstant").asString());
    }

    @Test
    void testRunToString() {
        assertEquals("ERROR", getDemonstrator().invokeMember("runToString").asString());
    }
}