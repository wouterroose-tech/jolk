package tolk.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkSwitchTest extends JolcTestBase {
    

    /// ## Switch Pattern Matching
    /// Demonstrates Jolk's message-based pattern matching which replaces the traditional
    /// switch statement with a fluid chain of `#case` selectors.
    @Test
    void testSwitch() {
        String source = """
            class SwithTest {
                String run(String color) {
                    ^ color
                        #case("red") #do ["RED"]
                        #case("blue") #do ["BLUE"]
                        #case("green") #do ["GREEN"]
                        #default ["unknown"];
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("RED", instance.invokeMember("run", "red").asString()); 
        assertEquals("BLUE", instance.invokeMember("run", "blue").asString()); 
        assertEquals("GREEN", instance.invokeMember("run", "green").asString()); 
        assertEquals("unknown", instance.invokeMember("run", "yellow").asString()); 
    }  
}
