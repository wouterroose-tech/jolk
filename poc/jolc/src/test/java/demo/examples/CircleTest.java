package demo.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.graalvm.polyglot.Value;

import tolk.JolcTestBase;

/// # CircleTest
///
/// Verifies the behavior of the `Circle` archetype and its geometric calculations
/// as orchestrated by the `CircleExample`.

public class CircleTest  extends JolcTestBase {

    @Test
    void testRunBasicNew() {
        Value circle = getCircleClass().invokeMember("new", 5.0);
        assertNotNull(circle);
        assertFalse(circle.isNull());
        assertEquals(5.0, circle.invokeMember("radius").asDouble());
    }

    @Test
    void testRunArea() {
        Value circle = getCircleClass().invokeMember("new", 5.0);
        Value area = circle.invokeMember("area");
        assertEquals(78.5398, area.asDouble(), 0.0001);
    }

    @Test
    @Disabled("lazy fields are not yet supported in the test setup")
    void testRunDiameter() {
        Value circle = getCircleClass().invokeMember("new", 5.0);
        Value diameter = circle.invokeMember("diameter");
        assertEquals(10.0, diameter.asDouble(), 0.0001);
    }

    @Test
    void testRunCircumference() {
        Value circle = getCircleClass().invokeMember("new", 5.0);
        Value circumference = circle.invokeMember("circumference");
        assertEquals(31.4159, circumference.asDouble(), 0.0001);
    }

    @Test
    void testRunToString() {
        Value circle = getCircleClass().invokeMember("new", 5.0);
        Value str = circle.invokeMember("toString");
        assertEquals("Circle[radius=5.0]", str.asString());
    }

    @Test
    void testRunInvalidConstruction() {
        assertThrows(Exception.class, () -> getCircleClass().invokeMember("new", -1.0) );
    } 

    private Value getCircleClass() {
        String source = """
        & java.lang.Math.PI;
        final class Circle {
            Double radius;
            lazy Double diameter = self #radius * 2;

            meta Circle new(Double r) {
                (r < 0) ? [ Exception #throw("Radius cannot be negative") ];
                ^ super #new #radius(r)
            }

            Double area() {
                ^ PI * self #radius ** 2
            }

            Double circumference() {
                ^ 2 * PI * self #radius
            }

            String toString() {
                ^ "Circle[radius=" + self #radius #toString + "]"
            }
        }""";
        return eval(source);
    }
}
