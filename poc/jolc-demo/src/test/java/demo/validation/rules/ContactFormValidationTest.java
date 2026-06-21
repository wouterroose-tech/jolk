package demo.validation.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContactFormValidationTest extends ValidationTestBase {

    @BeforeEach
    public void setUp() {
        super.setUp();
        setUp("/demo/validation/rules/ContactFormValidationTest.jolk");
    }

    @Test
    public void test_ValidateContactForm_success() {
        test("test_ValidateContactForm_success");
    }

    @Test
    public void test_ValidateContactForm_ZipConstraintIssue() {
        test("test_ValidateContactForm_ZipConstraintIssue");
    }

    @Test
    public void test_ValidateContactForm_SsnConstraintIssue() {
        test("test_ValidateContactForm_SsnConstraintIssue");
    }

    @Test
    public void test_interrupt() {
        test("test_interrupt");
    }

}