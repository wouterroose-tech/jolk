package demo.validation.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ContactFormTest {

    @Test
    public void testContactFormCreation() {
        Person p = new Person(1L, "FirstName", "LastName");
        ContactForm c = new ContactForm(p, "first contact", 2800); 
        assertNotNull(c);
        assertEquals(p, c.person());
        assertEquals("first contact", c.description());
    }
}
