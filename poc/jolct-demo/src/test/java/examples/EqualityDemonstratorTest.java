package examples;

import demo.validation.domain.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class EqualityDemonstratorTest {

    private final EqualityDemonstrator demo = new EqualityDemonstrator();
    private Person p1;
    private Person p2;
    private Person p3;

    @BeforeEach
    void setUp() {
        p1 = new Person().ssn(123).firstName("John").lastName("Doe");
        p2 = new Person().ssn(123).firstName("John").lastName("Doe");
        p3 = new Person().ssn(456).firstName("Jane").lastName("Doe");
    }

    @Test
    public void testRunEquals() {
        assertTrue(demo.runEquals(p1, p1)); // Same instance
        assertTrue(demo.runEquals(p1, p2)); // Different instances, same content
        assertFalse(demo.runEquals(p1, p3)); // Different content
    }

    @Test
    public void testRunNotEquals() {
        assertFalse(demo.runNotEquals(p1, p1));
        assertFalse(demo.runNotEquals(p1, p2));
        assertTrue(demo.runNotEquals(p1, p3));
    }

    @Test
    public void testRunEqualsWithNull() {
        assertFalse(demo.runEquals(p1, null));
        assertFalse(demo.runEquals(null, p1));
        assertTrue(demo.runEquals(null, null));
    }

    @Test
    public void testRunNotEqualsWithNull() {
        assertTrue(demo.runNotEquals(p1, null));
        assertTrue(demo.runNotEquals(null, p1));
        assertFalse(demo.runNotEquals(null, null));
    }
}