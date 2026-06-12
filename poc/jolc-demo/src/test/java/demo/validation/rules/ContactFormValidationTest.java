package demo.validation.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContactFormValidationTest extends ValidationTestBase {

    @BeforeEach
    public void setUp() {
        setUp("/demo/validation/rules/ContactFormValidationTest.jolk");
    }

    @Test
    public void test_ValidateContactForm_success() {
        assertTrue(testInstance.invokeMember("test_ValidateContactForm_success").asBoolean());
    }

    @Test
    public void test_ValidateContactForm_ZipConstraintIssue() {
        assertTrue(testInstance.invokeMember("test_ValidateContactForm_ZipConstraintIssue").asBoolean());
    }

    @Test
    public void test_ValidateContactForm_InssConstraintIssue() {
        assertTrue(testInstance.invokeMember("test_ValidateContactForm_SsnConstraintIssue").asBoolean());
    }

    @Test
    public void test_interrupt() {
        assertTrue(testInstance.invokeMember("test_interrupt").asBoolean());
    }

}