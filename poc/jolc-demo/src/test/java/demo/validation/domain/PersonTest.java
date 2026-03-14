package demo.validation.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PersonTest {

    @Test
    public void testPersonCreation() {
        // You are testing the Java class generated from Person.jolk
        Person p = new Person(1, "FirstName", "LastName");
        assertNotNull(p);
        assertEquals(1, p.ssn());
        assertEquals("FirstName", p.firstName());
        assertEquals("LastName", p.lastName());
    }
}
