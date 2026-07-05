package demo.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void testNew() {
        Value circle = getCircleClass().invokeMember("new", 5.0);
        assertNotNull(circle);
        assertFalse(circle.isNull());
        assertEquals(5.0, circle.invokeMember("radius").asDouble());
    }

    @Test
    @Disabled("lazy fields are not yet supported in the test setup")
    void testDiameter() {
        Value circle = getCircleClass().invokeMember("new", 5.0);
        Value diameter = circle.invokeMember("diameter");
        assertEquals(10.0, diameter.asDouble(), 0.0001);
    }

    @Test
    void testArea() {
        Value circle = getCircleClass().invokeMember("new", 5.0);
        Value area = circle.invokeMember("area");
        assertEquals(78.5398, area.asDouble(), 0.0001);
    }

    @Test
    void testCircumference() {
        Value circle = getCircleClass().invokeMember("new", 5.0);
        Value circumference = circle.invokeMember("circumference");
        assertEquals(31.4159, circumference.asDouble(), 0.0001);
    }

    @Test
    void testToString() {
        Value circle = getCircleClass().invokeMember("new", 5.0);
        Value str = circle.invokeMember("toString");
        assertEquals("Circle[radius=5.0]", str.asString());
    }
    
    @Test
    void testScaling() {
        Value circle = getCircleClass().invokeMember("new", 5.0);
        circle = circle.invokeMember("*", 2);
        Value radius = circle.invokeMember("radius");
        assertEquals(10, radius.asDouble(), 0.0001);
    }
    
    @Test
    void testEquivalence() {
        Value c1 = getCircleClass().invokeMember("new", 5.0);
        Value c2 = getCircleClass().invokeMember("new", 5.0);
        assertTrue(c1.invokeMember("~~", c2).asBoolean());
    }

    @Test
    void testInvalidConstruction() {
        assertThrows(Exception.class, () -> getCircleClass().invokeMember("new", -1.0) );
    } 

    private Value getCircleClass() {
        String source = """
        ~ examples;
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
            Circle *(Number factor) {
                ^ Circle #new(self #radius * factor)
            }
            Boolean ~~(Object other) {
                (self == other) ? [ ^ true ];
                // Use pattern matching to safely compare properties
                other #instanceOf(Circle)
                    #ifPresent [ c ->  ^ self #radius == c #radius ];
                ^ false
            }
        }""";
        return eval(source);
    }
}
