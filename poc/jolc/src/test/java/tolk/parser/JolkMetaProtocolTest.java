package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkMetaProtocolTest extends JolcTestBase {

    
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
                meta Long FORTY_TWO() { ^ 42 }
                Long classVal() { ^ self #class #FORTY_TWO }
                Long SelfVal() { ^ Self #FORTY_TWO }
                Long metaVal() { ^ MetaTest #FORTY_TWO }
            }""";
        Value meta = eval(source);
        assertEquals(42, meta.invokeMember("FORTY_TWO").asLong());
        Value instance = meta.invokeMember("new");
        assertEquals(42, instance.invokeMember("classVal").asLong());
        assertEquals(42, instance.invokeMember("SelfVal").asLong());
        assertEquals(42, instance.invokeMember("metaVal").asLong());
    }

    @Test
    void testMetaMethod_3() {
        String classA = "class ClassA { meta Long fortyTwo() { ^ 42 } }";
        String classB = "class ClassB { Long fortyTwo() { ^ ClassA #fortyTwo } }";
        eval(classA);
        Value instanceB = eval(classB).invokeMember("new");
        assertEquals(42, instanceB.invokeMember("fortyTwo").asLong());
    }

    @Test
    void testMetaFieldAccessors() {
        String source = """
            class MetaTest {
                meta Long META_VAL = 0;
            }""";
        Value meta = eval(source);
        // access meta constant 
        assertEquals(0, meta.invokeMember("META_VAL").asLong());
        assertEquals(meta, meta.invokeMember("META_VAL", 42));
        assertEquals(42, meta.invokeMember("META_VAL").asLong());
    }

    @Test
    void testMetaFieldsCrossClass() {
        String classA = "class ClassA { public meta constant Long FORTY_TWO = 42; }";
        String classB = "class ClassB { Long val() { ^ ClassA #FORTY_TWO } }";
        eval(classA);
        Value instanceB = eval(classB).invokeMember("new");
        // access projected meta constant 
        assertEquals(42, instanceB.invokeMember("val").asLong());
    }

    /**
     * Verifies the fallback resolution where a bare uppercase identifier
     * inside an instance method is resolved by sending a message to
     * the class object (the meta-receiver).
     */
    @Test
    void testResolveMetaConstantFallback() {
        String source = """
            class MetaFallbackTest {
                meta constant Long FORTY_TWO = 42;
                Long fortyTwo() { ^ FORTY_TWO }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertEquals(42, meta.invokeMember("FORTY_TWO").asLong());
        assertEquals(42, instance.invokeMember("fortyTwo").asLong());
    }

    @Test
    void testInstanceCreation() {
        String classA = "class ClassA { Long x = 42; }";
        String classB = "class ClassB { ClassA createA() { ^ClassA #new } }";
        eval(classA);
        Value instanceB = eval(classB).invokeMember("new");
        Value instanceA = instanceB.invokeMember("createA");
        assertEquals(42, instanceA.invokeMember("x").asLong());
    }

}
