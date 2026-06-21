package examples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.JolkTestBase;

/// # CircleTest
///
/// Verifies the behavior of the `Circle` archetype and its geometric calculations
/// as orchestrated by the `CircleExample`.

public class CircleTest  extends JolkTestBase {

    @BeforeEach
    public void setUp() {
        super.setUp();
        getJolkClass("/examples/Circle.jolk");
        super.setUp("/examples/CircleTest.jolk");
    }

    @Test
    void testNew() {
        test("testNew");
    }

    @Test
    void testCircleArea() {
        test("testCircleArea");
    }

    @Test
    void testCircumference() {
        test("testCircumference");
    }

    /*

    @Test
    void testRunToString() {
        Value str = getDemonstrator().invokeMember("runToString");
        assertEquals("Circle[radius=5.0]", str.asString());
    }

    /// ### testRunScaling
    ///
    /// Verifies that the scaling operator creates a new Circle with a scaled radius.
    @Test
    void testRunScaling() {
        Value scaled = getDemonstrator().invokeMember("runScaling");
        assertEquals(10.0, scaled.invokeMember("radius").asDouble());
    }

    /// ### testRunEquivalence
    ///
    /// Verifies that two different Circle instances with the same radius are equivalent.
    @Test
    void testRunEquivalence() {
        Value result = getDemonstrator().invokeMember("runEquivalence");
        assertTrue(result.asBoolean());
    }

    /// ### testRunInvalidConstruction
    ///
    /// Verifies that the construction guard throws an exception for negative radii.
    @Test
    void testRunInvalidConstruction() {
        assertThrows(Exception.class, () -> getDemonstrator().invokeMember("runInvalidConstruction"));
    }

    */
}
