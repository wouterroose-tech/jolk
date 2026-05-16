package demo.validation;

import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;


/// This test class is for testing domain classes of the Jolk Demo.
/// test parsing, creation and protocol

public class DomainTest extends JolcTestBase {

    private Value personClass() {
        String source = """
            class Person {
                Long ssn;
                String firstName;
                String lastName;
                String name() {
                    ^ self #firstName + " "+ self #lastName
                }
                String toString() {
                    ^ "Person[ssn=" + self #ssn + ", firstName=" + self #firstName + ", lastName=" + self #lastName + "]"
                }
                Boolean ~~(Object other) {
                    (self == other) ? [ ^true ];
                    other #instanceOf(Person) #ifPresent [ p -> 
                        ^ (self #ssn == p #ssn)
                            && (self #firstName ~~ p #firstName)
                            && (self #lastName ~~ p #lastName)
                        ];
                    ^ false
                }
            }""";
        return eval(source);
    }

    private Value contactFormClass() {
        String source = """
            final class ContactForm {
                Person person;
                String description;
                Long zipCode;
                String toString() {
                    ^ "ContactForm[person=" + self #person + ", description=" + self #description + ", zipCode=" + self #zipCode + "]"
                }
                Boolean ~~(Object other) {
                    (self == other) ? [ ^true ];
                    other #instanceOf(ContactForm) #ifPresent [ f -> 
                        ^ (self #person ~~ f #person)
                            && (self #description ~~ f #description)
                            && (self #zipCode ~~ f #zipCode)
                        ];
                    ^ false
                }
            }""";
        return eval(source);
    }

    @Test
    void testPerson() {
        Value person = this.personClass().invokeMember("new", 123456789, "John", "Doe");
        assertEquals(123456789, person.invokeMember("ssn").asLong());
        assertEquals("John", person.invokeMember("firstName").asString());
        assertEquals("Doe", person.invokeMember("lastName").asString());
        assertEquals("John Doe", person.invokeMember("name").asString());
        assertEquals("Person[ssn=123456789, firstName=John, lastName=Doe]", person.invokeMember("toString").asString());
        assertTrue(person.invokeMember("~~", person).asBoolean());
        Value person2 = this.personClass().invokeMember("new", 123456789, "John", "Doe");
        assertTrue(person.invokeMember("~~", person2).asBoolean());
        Value person3 = this.personClass().invokeMember("new", 0L, "X", "X");
        assertFalse(person.invokeMember("~~", person3).asBoolean());
    }

    @Test
    void testContactForm() {
        Value person = this.personClass().invokeMember("new", 123456789, "John", "Doe");
        Value form = this.contactFormClass().invokeMember("new", person, "Form", 1234L);

        assertEquals(person, form.invokeMember("person"));
        assertEquals("Form", form.invokeMember("description").asString());
        assertEquals(1234L, form.invokeMember("zipCode").asLong());

        // Verify string representation (validates protocol interpolation)
        assertEquals("ContactForm[person=Person[ssn=123456789, firstName=John, lastName=Doe], description=Form, zipCode=1234]", 
                     form.invokeMember("toString").asString());

        // Verify structural equivalence
        assertTrue(form.invokeMember("~~", form).asBoolean(), "Form must be equivalent to itself.");
        
        Value person2 = this.personClass().invokeMember("new", 123456789, "John", "Doe");
        Value form2 = this.contactFormClass().invokeMember("new", person2, "Form", 1234L);
        assertTrue(form.invokeMember("~~", form2).asBoolean(), "Forms with equivalent data must be equivalent.");

        Value form3 = this.contactFormClass().invokeMember("new", person, "Other", 9999L);
        assertFalse(form.invokeMember("~~", form3).asBoolean(), "Forms with different data must not be equivalent.");
    }

}
