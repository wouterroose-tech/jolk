package demo.validation.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import util.JolkTestBase;

import org.graalvm.polyglot.Value;

public class PersonTest extends JolkTestBase {

    @Test
    public void testPersonCreation() {
        Value personClass = getPersonClass();
        Value person = personClass.invokeMember("new" , 123456789, "John", "Doe");
        assertEquals(123456789, person.invokeMember("ssn").asLong());
        assertEquals("John", person.invokeMember("firstName").asString());
        assertEquals("Doe", person.invokeMember("lastName").asString());
        assertEquals("John Doe", person.invokeMember("name").asString());
    }

    /// testPersonEquality
    ///
    /// Verifies the behavior of the `==` (identity) operator for Person objects.
    @Test
    public void testPersonEquality() {
        Value personClass = getPersonClass();
        Value person1 = personClass.invokeMember("new", 123456789, "John", "Doe");
        Value person2 = personClass.invokeMember("new", 123456789, "John", "Doe");

        // Identity: same object reference
        assertTrue(person1.invokeMember("==", person1).asBoolean(), "Person should be identical to itself.");
        // Identity: different object references, even with same data
        assertFalse(person1.invokeMember("==", person2).asBoolean(), "Two distinct Person objects should not be identical.");
    }

    /// testPersonEquivalence
    ///
    /// Verifies the behavior of the `~~` (equivalence) operator for Person objects,
    /// which is overridden in Person.jolk for structural comparison.
    @Test
    public void testPersonEquivalence() {
        Value personClass = getPersonClass();
        Value person1 = personClass.invokeMember("new", 123456789, "John", "Doe");
        Value person2 = personClass.invokeMember("new", 123456789, "John", "Doe");
        Value person3 = personClass.invokeMember("new", 987654321, "Jane", "Smith");

        // Equivalence: same object reference
        assertTrue(person1.invokeMember("~~", person1).asBoolean(), "Person should be equivalent to itself.");
        // Equivalence: different object references but same data (due to overridden ~~ in Person.jolk)
        assertTrue(person1.invokeMember("~~", person2).asBoolean(), "Two distinct Person objects with same data should be equivalent.");
        // Equivalence: different data
        assertFalse(person1.invokeMember("~~", person3).asBoolean(), "Two Person objects with different data should not be equivalent.");
    }

    private Value getPersonClass() {
        return load("/demo/validation/domain/Person.jolk");
    }
}
