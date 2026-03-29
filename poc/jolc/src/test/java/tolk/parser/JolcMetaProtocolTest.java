package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolcMetaProtocolTest extends JolcTestBase {

    
    @Test
    void testMetaMethod() {
        String source = "class MetaTest { meta Long val() { ^ 42 } }";
        Value meta = eval(source);
        assertEquals(42, meta.invokeMember("val").asLong());
    }

    
    @Test
    @Disabled("Activate when meta dispatch is implemented")
    void testMetaMethod_2() {
        String classA = "class ClassA { meta Long val() { ^ 42 } }";
        String classB = "class ClassB { Long val() { ^ ClassA #val } }";
        eval(classA);
        Value instanceB = eval(classB).invokeMember("new");
        assertEquals(42, instanceB.invokeMember("val").asLong());
    }

}
