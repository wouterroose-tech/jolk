package demo.validation.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class ContactFormValidationTest  extends JolcTestBase {

    private Value interrupt() {
        String source = """
            ~ test;
            class Interrupt extends RuntimeException {

                meta constant Interrupt HALT = Interrupt #new;

                meta Interrupt new() {
                    ^super #new("Validation Halt", null, false, false)
                }   
            }""";
        return eval(source);
    }

    private Value contactFormValidation() {
        String source = """
            ~ test;
            class ContactFormValidation {
                    meta constant Interrupt FORM_INTERRUPT = Interrupt #new;
            }""";
        return eval(source);
    }

    private Value contactFormValidationTest() {
        String source = """
            ~ test;
            & test.ContactFormValidation.FORM_INTERRUPT;
            class ContactFormValidationTest {

                Boolean test_interrupt() {
                    ^ ContactFormValidation #FORM_INTERRUPT #isPresent 
                }

                Boolean test_interrupt_projection() {
                    ^ FORM_INTERRUPT #instanceOf(Interrupt)#isPresent 
                }
            }""";
        return eval(source);
    }

    @Test
    void testParsing() {
        this.interrupt();
        this.contactFormValidation();
        this.contactFormValidationTest();
    }
    
    /// ### test_interrupt_meta
    ///
    /// Verifies the identity of the `FORM_INTERRUPT` constant using the Jolk 
    /// messaging protocol rather than host-level reflection. This avoids 
    /// `ClassCastException` by asserting on the behavioral contract 
    /// provided by `#instanceOf`.
    @Test
    public void test_interrupt_meta() {
        Value interruptClass = this.interrupt();
        Value constant = this.contactFormValidation().getMember("FORM_INTERRUPT");

        // Verifying identity via the Jolk Object Protocol: #instanceOf returns a Match identity.
        Value match = constant.invokeMember("instanceOf", interruptClass);
        assertTrue(match.invokeMember("isPresent").asBoolean(), 
            "FORM_INTERRUPT should be recognized as an instance of Interrupt within the Jolk protocol.");
        
        // Verifying the class name via the Meta-Object Protocol.
        assertEquals("Interrupt", constant.invokeMember("class").invokeMember("name").asString());
    }
    
    @Test
    public void test_interrupt_meta_projection() {
        this.interrupt();
        this.contactFormValidation();
        Value testInstance = this.contactFormValidationTest().invokeMember("new");
        assertTrue(testInstance.invokeMember("test_interrupt").asBoolean());
        assertTrue(testInstance.invokeMember("test_interrupt_projection").asBoolean());
    }

}