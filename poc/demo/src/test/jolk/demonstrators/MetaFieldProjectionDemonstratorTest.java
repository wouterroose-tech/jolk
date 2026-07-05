package demonstrators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.graalvm.polyglot.Value;

import util.JolkTestBase;

/// # MetaFieldProjectionDemonstratorTest
///
/// Verifies the interaction with Meta-Level Projection lenses as showcased 
/// by the [MetaFieldProjectionDemonstrator].
public class MetaFieldProjectionDemonstratorTest extends JolkTestBase {

    private Value getDemonstrator() {
        Value demonstrator = load("/demonstrators/MetaFieldProjectionDemonstrator.jolk");
        return demonstrator.invokeMember("new");
    }

    /// ### testCalculateCircumference
    ///
    /// Verifies calculation using the `TWO_PI` lens projected from `java.lang.Math.TAU`.
    @Test
    void testCalculateCircumference() {
        Value demo = getDemonstrator();
        double radius = 5.0;
        assertEquals(radius * 2.0 * Math.PI, demo.invokeMember("calculateCircumference", radius).asDouble(), 1e-9);
    }

    /// ### testCalculateArea
    ///
    /// Verifies calculation using the `PI` lens projected from `java.lang.Math.PI`.
    @Test
    void testCalculateArea() {
        Value demo = getDemonstrator();
        double radius = 5.0;
        assertEquals(Math.PI * Math.pow(radius, 2), demo.invokeMember("calculateArea", radius).asDouble(), 1e-9);
    }
}
