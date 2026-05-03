package tolk.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkNumberTest extends JolcTestBase {
    

    @Test
    void testGuidedCoercion() {
        // Mixing Double and Long should trigger automatic promotion to Double (Passive Coercion)
        String source = """
            class CoercionTest {
                Double mixedAdd(Double d, Long l) { ^ d + l }
                Double mixedMul(Long l, Double d) { ^ l * d }
                Boolean mixedEq(Double d, Long l) { ^ d == l }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(11.5, instance.invokeMember("mixedAdd", 1.5, 10L).asDouble());
        assertEquals(25.0, instance.invokeMember("mixedMul", 10L, 2.5).asDouble());
        assertTrue(instance.invokeMember("mixedEq", 10.0, 10L).asBoolean());
    }

}
