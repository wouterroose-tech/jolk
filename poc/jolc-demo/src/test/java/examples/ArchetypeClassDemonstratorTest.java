package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.graalvm.polyglot.Value;

import util.JolkTestBase;

public class ArchetypeClassDemonstratorTest extends JolkTestBase {

    private Value getDemonstrator() {
        getJolkClass("/demo/validation/domain/Person.jolk");
        Value demonstrator = getJolkClass("/examples/ArchetypeClassDemonstrator.jolk");
        return demonstrator.invokeMember("new");
    }

    /// ### testRunBasicNew
    /// 
    /// Verifies the basic instantiation of the `Person` archetype.
    @Test
    void testRunBasicNew() {
        Value person = getDemonstrator().invokeMember("runBasicNew");
        assertNotNull(person);
        assertFalse(person.isNull());
    }

    /// ### testRunCanonicalNew
    ///
    /// Verifies the canonical instantiation of `Person` with state.
    @Test
    void testRunCanonicalNew() {
        Value person = getDemonstrator().invokeMember("runCanonicalNew");
        assertEquals(123456789L, person.invokeMember("ssn").asLong());
        assertEquals("John", person.invokeMember("firstName").asString());
        assertEquals("Doe", person.invokeMember("lastName").asString());
    }

    @Test
    void testRunVariable() {
        Value demonstrator = getDemonstrator();
        assertEquals("Jane", demonstrator.invokeMember("runVariable").asString());
    }

    @Test
    void testRunConstant() {
        Value demonstrator = getDemonstrator();
        assertEquals("Jane", demonstrator.invokeMember("runConstant").asString());
    }

    @Test
    void testRunFluid() {
        Value demonstrator = getDemonstrator();
        assertEquals("Jane Doe", demonstrator.invokeMember("runFluid").asString());
    }

    @Test
    void testRunToString() {
        Value demonstrator = getDemonstrator();
        assertEquals("Person[ssn=123456789, firstName=John, lastName=Doe]", demonstrator.invokeMember("runToString").asString());
    }
}
