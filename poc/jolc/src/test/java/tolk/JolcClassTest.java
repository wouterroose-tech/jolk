package tolk;

import org.graalvm.polyglot.Value;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class JolcClassTest extends JolcTestBase {

    @Test
    public void testClassDefinition() {
        Value result = eval("class Test { }");
        assertFalse(result.isNull(), "Defining a class should not return null in the current implementation");
    }
}
