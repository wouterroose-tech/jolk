package demo.validation.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import demo.validation.rules.ValidationTestBase;

import org.graalvm.polyglot.Value;

public class ContactFormTest extends ValidationTestBase {

    @BeforeEach
    public void setUp() {
        setUp("demo/validation/domain/ContactFormTest.jolk");
    }

    @Test
    public void testNew() {
        assertTrue(test("testNew").asBoolean());
    }
    
    @Test
    public void testNewCompleted() {
        assertTrue(test("testNewCompleted").asBoolean());

    }

    /// Verifies the behavior of the `==` (identity) operator for ContactForm objects.
    @Test
    public void testEquality() {
        assertTrue(test("testEquality1").asBoolean());
        assertTrue(test("testEquality2").asBoolean());
    }

    /// Verifies the behavior of the `~~` (equivalence) operator for ContactForm objects.
    @Test
    public void testEquivalence() {
        assertTrue(test("testEquivalence1").asBoolean());
        assertTrue(test("testEquivalence2").asBoolean());
        assertTrue(test("testEquivalence3").asBoolean());
    }

    private Value test(String testCase) {
        return testInstance.invokeMember(testCase);
    }
}
