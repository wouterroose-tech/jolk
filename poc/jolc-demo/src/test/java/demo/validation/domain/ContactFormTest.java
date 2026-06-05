package demo.validation.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import util.JolkTestBase;

import org.graalvm.polyglot.Value;

public class ContactFormTest extends JolkTestBase {

    @Test
    public void testContactFormCreation() {
        Value contactFormClass = getContactFormClass();
        Value person = getPersonClass().invokeMember("new" , 123456789, "John", "Doe");
        Value contactForm = contactFormClass.invokeMember("new", person, "I've got a problem", 9999L, "alice@example.com");
        assertEquals(person, contactForm.invokeMember("person"));
        assertEquals("I've got a problem", contactForm.invokeMember("description").asString());
        assertEquals(9999L, contactForm.invokeMember("zipCode").asLong());
        assertEquals("alice@example.com", contactForm.invokeMember("email").asString());

        String expectedToString = "ContactForm[person=" + person.invokeMember("toString").asString() + ", description=I've got a problem, zipCode=9999, email=alice@example.com]";
        assertEquals(expectedToString, contactForm.invokeMember("toString").asString());
    }

    /// testContactFormEquality
    ///
    /// Verifies the behavior of the `==` (identity) operator for ContactForm objects.
    @Test
    public void testContactFormEquality() {
        Value contactFormClass = getContactFormClass();
        Value person = getPersonClass().invokeMember("new", 123456789, "John", "Doe");

        Value form1 = contactFormClass.invokeMember("new", person, "problem", 9999L, "a@b.com");
        Value form2 = contactFormClass.invokeMember("new", person, "problem", 9999L, "a@b.com");

        assertTrue(form1.invokeMember("==", form1).asBoolean(), "Form should be identical to itself.");
        assertFalse(form1.invokeMember("==", form2).asBoolean(), "Distinct forms should not be identical.");
    }

    /// testContactFormEquivalence
    ///
    /// Verifies the behavior of the `~~` (equivalence) operator for ContactForm objects.
    @Test
    public void testContactFormEquivalence() {
        Value contactFormClass = getContactFormClass();
        Value personClass = getPersonClass();

        Value person1 = personClass.invokeMember("new", 123456789, "John", "Doe");
        Value person2 = personClass.invokeMember("new", 123456789, "John", "Doe");

        Value form1 = contactFormClass.invokeMember("new", person1, "problem", 9999L, "a@b.com");
        Value form2 = contactFormClass.invokeMember("new", person2, "problem", 9999L, "a@b.com");
        Value form3 = contactFormClass.invokeMember("new", person1, "different", 9999L, "a@b.com");

        assertTrue(form1.invokeMember("~~", form1).asBoolean(), "Form should be equivalent to itself.");
        assertTrue(form1.invokeMember("~~", form2).asBoolean(), "Forms with equivalent persons and matching data should be equivalent.");
        assertFalse(form1.invokeMember("~~", form3).asBoolean(), "Forms with different data should not be equivalent.");
    }

    private Value getContactFormClass() {
        return getJolkClass("/demo/validation/domain/ContactForm.jolk");
    }

    private Value getPersonClass() {
        return getJolkClass("/demo/validation/domain/Person.jolk");
    }
    
    @Test
    public void testNewContactForm() {
        getContactFormClass();
        getPersonClass();
        Value test = getContactFormTestClass().invokeMember("new");

        Value form = test.invokeMember("test_ValidateContactForm");

    }
    
    @Test
    public void testValidateContactForm() {
        getContactFormClass();
        getPersonClass();
        Value test = getContactFormTestClass().invokeMember("new");

        Value form = test.invokeMember("test_ValidateContactForm");

        assertEquals("john.doe@example.com", form.invokeMember("email").asString());
    }

    private Value getContactFormTestClass() {
        return getJolkClass("/test/validation/rules/ContactFormTest.jolk");
    }
}
