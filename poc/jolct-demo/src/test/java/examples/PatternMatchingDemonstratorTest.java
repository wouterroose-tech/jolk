package examples;

import demo.validation.domain.Person;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PatternMatchingDemonstratorTest {

    private final PatternMatchingDemonstrator demonstrator = new PatternMatchingDemonstrator();
    private final Person person = new Person().firstName("Jane").lastName("Doe").ssn(123);

    @Test
    void testRunInstanceOf() {
        assertTrue(demonstrator.runInstanceOf(person));
        assertFalse(demonstrator.runInstanceOf(null));
    }

    @Test
    void testRunInstanceOfIsPresent() {
        assertTrue(demonstrator.runInstanceOfIsPresent(person));
        assertFalse(demonstrator.runInstanceOfIsPresent(new jolk.lang.Object()));
    }

    @Test
    void testRunInstanceOfIfPresent() {
        assertEquals("Got a Person: " + person, demonstrator.runInstanceOfIfPresent(person));
        assertEquals("Not a Person", demonstrator.runInstanceOfIfPresent(new jolk.lang.Object()));
    }

    @Test
    void testRunAs() {
        assertEquals(person.toString(), demonstrator.runAs(person));
        assertThrows(ClassCastException.class, () -> demonstrator.runAs(new jolk.lang.Object()));
    }

    @Test
    void testRunAsIfPresent() {
        assertEquals("Got a Person: " + person, demonstrator.runAsIfPresent(person));
        assertEquals("Cast failed or was null", demonstrator.runAsIfPresent(new jolk.lang.Object()));
    }

    @Test
    void testRunIsInstance() {
        assertTrue(demonstrator.runIsInstance(person));
        assertFalse(demonstrator.runIsInstance(null));
    }
}
