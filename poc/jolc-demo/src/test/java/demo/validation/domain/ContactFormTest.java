package demo.validation.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import demo.validation.rules.ValidationTestBase;

public class ContactFormTest extends ValidationTestBase {

    @BeforeEach
    public void setUp() {
        super.setUp();
        setUp("demo/validation/domain/ContactFormTest.jolk");
    }

    @Test
    public void testNew() {
        test("testNew");
    }
    
    @Test
    public void testNewCompleted() {
        test("testNewCompleted");

    }

    /// Verifies the behavior of the `==` (identity) operator for ContactForm objects.
    @Test
    public void testEquality() {
        test("testEquality1");
        test("testEquality2");
    }

    /// Verifies the behavior of the `~~` (equivalence) operator for ContactForm objects.
    @Test
    public void testEquivalence() {
        test("testEquivalence1");
        test("testEquivalence2");
        test("testEquivalence3");
    }
}
