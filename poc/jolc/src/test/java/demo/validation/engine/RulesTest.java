package demo.validation.engine;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class RulesTest extends JolcTestBase {

    private Value interrupt() {
        String source = """
            final class Interrupt extends RuntimeException {
                meta constant Interrupt HALT = Interrupt #new;
                meta Interrupt new() {
                    // Removes the overhead; the identity is now a lightweight flow-control signal.
                    // Disable stack trace (writableStackTrace' = false) for performance.
                    ^super #new("Validation Halt", null, false, false)
                }
            }""";
        return eval(source);
    }

    private Value contactFormValidation() {
        String source = """
            class ContactFormValidation extends ValidationSuite<ContactForm> {
                meta constant Interrupt FORM_INTERRUPT = Interrupt #new;

                meta lazy ContactFormValidation new() {
                    ^ super #new
                        #add(ZipConstraint #new)
                        #add(SsnConstraint #new, ContactForm ##person)
                }
            }""";
        return eval(source);
    }

    private Value zipConstraint() {
        String source = """
            #! class ZipConstraint extends Constraint<ContactForm> {
                Boolean satisfiesPreCondition(ContactForm form, ExecutionContext executionContext) {
                    ^ form #zipCode #isPresent
                }
                #: Boolean isValid(ContactForm form) {
                    ^ self #isMalinesArea(form #zipCode)
                }
                #> Boolean isMalinesArea(Int zipCode) {
                    ^ GeoGraphicalService #GGS #exists(zipCode) && MECHELEN #contains(zipCode)
                }
                #: Issue getIssue(ContactForm form,  ExecutionContext executionContext) {
                    ^ Issue #new(form, "ZIP_INVALID",  Level #ERROR)
                }
                #: Interrupt interrupt() {
                    ^ FORM_INTERRUPT
                }
            }""";
        return eval(source);
    }

    private Value ssnConstraint() {
        String source = """
            #! class SsnConstraint extends Constraint<Person> {
                Boolean satisfiesPreCondition(Person person, ExecutionContext executionContext) {
                    ^ person #ssn #isPresent
                }
                #: Boolean isValid(Person person) {
                    ^ self #isValid(person #ssn)
                }
                #> Boolean isValid(Long ssn) {
                    ^ (ssn / 100 / 97) != (ssn % 97)
                }
                #: Issue getIssue(Person person,  ExecutionContext executionContext) {
                    ^ Issue #new(person, "SSN_INVALID",  Level #ERROR)
                }
                #: Interrupt interrupt() {
                    ^ ContactFormValidation #FORM_INTERRUPT
                }
            }""";
        return eval(source);
    }   

    @Test
    void testParsing() {
        this.interrupt();
        this.zipConstraint();
        this.ssnConstraint();
        this.contactFormValidation();
    }

}
