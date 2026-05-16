package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.graalvm.polyglot.Value;

import util.JolkTestBase;

/// # CircleTest
///
/// Verifies the behavior of the `Circle` archetype and its geometric calculations
/// as orchestrated by the `CircleExample`.

public class CircleExampleTest  extends JolkTestBase {


    private Value getDemonstrator() {
        // Ensure the domain class is registered before running the demonstrator
        getJolkClass("/examples/Circle.jolk");
        Value demonstrator = getJolkClass("/examples/CircleExample.jolk");
        return demonstrator.invokeMember("new");
    }

    /// ### testRunBasicNew
    /// 
    /// Verifies the basic instantiation of a `Circle`.
    @Test
    void testRunBasicNew() {
        Value circle = getDemonstrator().invokeMember("runBasicNew");
        assertNotNull(circle);
        assertFalse(circle.isNull());
        assertEquals(5.0, circle.invokeMember("radius").asDouble());
    }

    /// ### testRunArea
    ///
    /// Verifies the area calculation: PI * r^2.
    @Test
    void testRunArea() {
        Value area = getDemonstrator().invokeMember("runArea");
        assertEquals(78.5398, area.asDouble(), 0.0001);
    }

    /// ### testRunCircumference
    ///
    /// Verifies the circumference calculation: 2 * PI * r.
    @Test
    void testRunCircumference() {
        Value circumference = getDemonstrator().invokeMember("runCircumference");
        assertEquals(31.4159, circumference.asDouble(), 0.0001);
    }

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
        assertThrows(Exception.class, () -> {
            getDemonstrator().invokeMember("runInvalidConstruction");
        });
    }
}
