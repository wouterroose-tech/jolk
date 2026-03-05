package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import demo.validation.domain.Person;

public class ArchetypeDemonstratorTest {

    private ArchetypeDemonstrator demonstrator = new ArchetypeDemonstrator();

    @Test
    void testRunBasicNew() {
        Person person = demonstrator.runBasicNew();
        assertNotNull(person);
    }

    @Test
    void testRunCanonicalNew() {
        Person person = demonstrator.runCanonicalNew();
        assertEquals(123456789, person.ssn());
        assertEquals("John", person.firstName());
        assertEquals("Doe", person.lastName());
    }

    @Test
    void testRunVariable() {
        assertEquals("Jane", demonstrator.runVariable());
    }

    @Test
    void testRunConstant() {
        assertEquals("Jane", demonstrator.runConstant());
    }

    @Test
    void testRunFluid() {
        assertEquals("Jane Doe", demonstrator.runFluid());
    }

    @Test
    void testRunToString() {
        assertEquals("Person[ssn=123456789, firstName=John, lastName=Doe]", demonstrator.runToString());
    }
}
