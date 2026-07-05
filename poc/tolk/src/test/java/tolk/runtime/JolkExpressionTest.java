package tolk.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkExpressionTest extends JolcTestBase {

    @Test
    void testBetweem() {
        String source = """
            class Test  {
                Boolean x(Int x) { ^ 0 < x && x < 100 }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(true, instance.invokeMember("x", 42).asBoolean());
    } 

}
