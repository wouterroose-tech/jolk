package tolk.parser;

import org.graalvm.polyglot.Value;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolcClassTest extends JolcTestBase {

    @Test
    @Disabled("Pending implementation of class definitions in Jolk.")
    public void testClassDefinition() {
        Value result = eval("class Test { }");
        assertFalse(result.isNull(), "Defining a class should not return null in the current implementation");
    }

    @Test
    @Disabled("Pending implementation of class definitions in Jolk.")
    void testVisitClassWithMethodAndField() {
        String source = """
            class MyClass {
                String name;
                Int calculate(Int a, Int b) {
                    ^ a + b
                }
            }
            """;
    }
}
