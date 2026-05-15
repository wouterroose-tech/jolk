package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.graalvm.polyglot.Value;

import util.JolkTestBase;

public class ArchetypeRecordDemonstratorTest extends JolkTestBase {

    private Value getDemonstrator() {
        getJolkClass("/demo/validation/engine/Level.jolk");
        getJolkClass("/demo/validation/engine/Issue.jolk");
        Value demonstrator = getJolkClass("/examples/ArchetypeRecordDemonstrator.jolk");
        return demonstrator.invokeMember("new");
    }

    /// ### testRunCanonicalNew
    ///
    /// Verifies the canonical instantiation of the `Issue` record with state.
    @Test
    void testRunCanonicalNew() {
        Value issue = getDemonstrator().invokeMember("runCanonicalNew");
        assertNotNull(issue);
        assertFalse(issue.isNull(), "Issue record should not be 'Nothing'.");

        assertEquals("Identity", issue.invokeMember("subject").asString());
        assertEquals("Validation failed", issue.invokeMember("message").asString());
        // Verify the Level enum constant name
        assertEquals("ERROR", issue.invokeMember("level").invokeMember("name").asString());
    }

    /// ### testRunAccessors
    ///
    /// Verifies that record components can be accessed via synthesized message selectors.
    @Test
    void testRunAccessors() {
        assertEquals("Message Content", getDemonstrator().invokeMember("runAccessors").asString());
    }

    @Test
    void testRunVariable() {
        assertEquals("Variable Test", getDemonstrator().invokeMember("runVariable").asString());
    }

    @Test
    void testRunConstant() {
        assertEquals("Constant Test", getDemonstrator().invokeMember("runConstant").asString());
    }

    /// ### testRunToString
    ///
    /// Verifies the synthesized string representation of the record archetype.
    @Test
    void testRunToString() {
        assertEquals("Issue[subject=Target, message=String representation, level=INFO]", getDemonstrator().invokeMember("runToString").asString());
    }
}
