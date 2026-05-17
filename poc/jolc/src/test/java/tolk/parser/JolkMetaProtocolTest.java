package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                Long SelfVal() { ^ Self #FORTY_TWO }
                Long metaVal() { ^ MetaTest #FORTY_TWO }
                Long classVal() { ^ self #class #FORTY_TWO }
            }""";
        Value meta = eval(source);
        assertEquals(42, meta.invokeMember("FORTY_TWO").asLong());
        Value instance = meta.invokeMember("new");
        assertEquals(42, instance.invokeMember("SelfVal").asLong());
        assertEquals(42, instance.invokeMember("metaVal").asLong());
        assertEquals(42, instance.invokeMember("classVal").asLong());
    }

    @Test
    void testMetaMethod_3() {
        String classA = "class ClassA { meta Long FortyTwo() { ^ 42 } }";
        String classB = "class ClassB { Long fortyTwo() { ^ ClassA #FortyTwo } }";
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
        assertEquals(meta, meta.invokeMember("META_VAL", 42L)); // Ensure Long literal for consistency
        assertEquals(42, meta.invokeMember("META_VAL").asLong());
    }

    /**
     * Verifies that the Lexical Fence prevents mutation of meta constants.
     */
    @Test
    void testMetaConstantImmutability() {
        String source = """
            class ConstTest {
                meta constant Long VERSION = 1;
            }""";
        Value meta = eval(source);
        assertThrows(Exception.class, () -> meta.invokeMember("VERSION", 2L));
    }

    @Test
    void testMetaFieldsCrossClass() {
        String classA = "class ClassA { meta constant Long FORTY_TWO = 42; }";
        String classB = "class ClassB { Long val() { ^ ClassA #FORTY_TWO } }";
        eval(classA);
        Value instanceB = eval(classB).invokeMember("new");
        // access meta constant 
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

    /// Verifies that a meta constant can be accessed from another class via the meta-receiver, 
    /// demonstrating cross-class meta field access. This also tests the fallback resolution mechanism
    /// for bare uppercase identifiers in instance methods, ensuring they are correctly resolved to
    /// the meta-receiver's fields.
    @Test
    void testMetaFieldProjection() {
        String classA = "class ClassA { meta constant Long FORTY_TWO = 42; }";
        String classB = """
            & ClassA.FORTY_TWO;
            & java.lang.Math.PI;
            class ClassB {
                Long val() { ^ FORTY_TWO }
                Double pi() { ^ PI }
            }""";
        eval(classA);
        Value instanceB = eval(classB).invokeMember("new");
        // access projected meta constant 
        assertEquals(42, instanceB.invokeMember("val").asLong());
        assertEquals(Math.PI, instanceB.invokeMember("pi").asDouble());
    }

    /// Verifies that a meta constant can be accessed from another class via the meta-receiver, 
    /// demonstrating cross-class meta field access. This also tests the fallback resolution mechanism.
    /// The `&` syntax is used to project the meta constant into the class scope, allowing it to be accessed
    /// directly without needing to reference the meta-receiver explicitly.
    @Test
    void testMetaFieldAlias() {
        String classA = "class ClassA { meta constant Long FORTY_TWO = 42; }";
        String classB = """
            & FORTYTWO = ClassA.FORTY_TWO;
            & MY_PI = java.lang.Math.PI;
            class ClassB {
                Long val() { ^ FORTYTWO }
                Double pi() { ^ MY_PI }
            }""";
        eval(classA);
        Value instanceB = eval(classB).invokeMember("new");
        // access projected meta constant 
        assertEquals(42, instanceB.invokeMember("val").asLong());
        assertEquals(Math.PI, instanceB.invokeMember("pi").asDouble());
    }

    /// ### testMetaProtocolIntrospection
    ///
    /// Verifies that a Meta-Object can report its own meta-level handshake 
    /// surface via the #metaProtocol message.
    @Test
    void testMetaProtocolIntrospection() {
        String source = "class IntroTest { meta Long FortyTwo() { ^ 42 } }";
        Value meta = eval(source);
        Value protocol = meta.invokeMember("metaProtocol");
        
        // The protocol should contain our custom meta-method and the intrinsic #new
        String protocolStr = protocol.toString();
        assertTrue(protocolStr.contains("FortyTwo"), "Meta-protocol must include defined meta-methods.");
        assertTrue(protocolStr.contains("new"), "Meta-protocol must include the intrinsic #new factory.");
    }

    /// ### testInstanceProtocolIntrospection
    ///
    /// Verifies that a Meta-Object reports the handshake surface for its 
    /// instances via the #instanceProtocol message.
    @Test
    void testInstanceProtocolIntrospection() {
        String source = "class IntroTest { Long fortyTwo() { ^ 42 } }";
        Value meta = eval(source);
        Value protocol = meta.invokeMember("instanceProtocol");
        
        assertTrue(protocol.toString().contains("fortyTwo"), "Instance protocol must include defined instance methods.");
    }

    /// ### testDynamicIdentityProjection
    ///
    /// Verifies the Dynamic Message Send API: using the meta-layer to project 
    /// an identity (message) onto a receiver dynamically via a string.
    @Test
    void testDynamicIdentityProjection() {
        String source = """
            class Target {
                Long val = 0;
                Long update(Long v) {
                    self #val(v);
                    ^ self #val }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // Reify a string into a selector and project it onto the instance
        // Jolk: selector = Target #message("update"); instance #project(selector, 100)
        Value result = instance.invokeMember("project", "update", 100L);
        assertEquals(100L, result.asLong());
    }

}
