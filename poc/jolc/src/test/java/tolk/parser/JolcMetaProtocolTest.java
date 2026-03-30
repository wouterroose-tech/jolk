package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
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
    void testMetaMethod_2() {
        String source = """
            class MetaTest {
                meta Long VAL() { ^ 42 }
                Long classVal() { ^ self #class #VAL }
                Long SelfVal() { ^ Self #VAL }
                Long metaVal() { ^ MetaTest #VAL }
            }""";
        Value meta = eval(source);
        assertEquals(42, meta.invokeMember("VAL").asLong());
        Value instance = meta.invokeMember("new");
        assertEquals(42, instance.invokeMember("classVal").asLong());
        assertEquals(42, instance.invokeMember("SelfVal").asLong());
        assertEquals(42, instance.invokeMember("metaVal").asLong());
    }

    @Test
    void testMetaMethod_3() {
        String classA = "class ClassA { meta Long val() { ^ 42 } }";
        String classB = "class ClassB { Long val() { ^ ClassA #val } }";
        eval(classA);
        Value instanceB = eval(classB).invokeMember("new");
        assertEquals(42, instanceB.invokeMember("val").asLong());
    }

}
