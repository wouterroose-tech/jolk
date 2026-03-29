package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolcCollectionsTest extends JolcTestBase {

    @Test
    @Disabled("Pending implementation of Map & String") 
    void testNullRespondsToCoreProtocol() {
        String source = """
            class MyClass {
                Map<Long, String> map = #(
                    1 -> "one",
                    2 -> "two",
                    3 -> "three");
                String run(Int key) { ^ map #at(key) }            
        """;
        Value instance = eval(source);
        assertEquals("one", instance.invokeMember("get", 1));
        assertNull(instance.invokeMember("get", 0));

    }

}
