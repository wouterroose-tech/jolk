package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolcBindingTest  extends JolcTestBase {
    
    @Test
    void testFieldInitialisation() {
        String source = "class MyClass { Long x = 42;}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // access via synthesized accessor
        assertEquals(42L, instance.invokeMember("x").asLong());
    }
    
    @Test
    @Disabled("activate when field accessors are implemented")
    void testFieldAccessors() {
        String source = "class MyClass { Long x;}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // access via synthesized accessor
        assertEquals(0L, instance.invokeMember("x").asLong());
        assertEquals(instance, instance.invokeMember("x", 42L).asLong());
        assertEquals(42L, instance.invokeMember("x").asLong());
    }

    @Test
    void testFieldAccess() {
        String source = "class MyClass { Long x = 42; Long val() { ^ x } }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // field access in method
        assertEquals(42L, instance.invokeMember("val").asLong());
    }

    @Test
    void testVariableInitialization() {
        String source = "class MyClass { Long val() { Long x = 42; ^ x } }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // field access in method
        assertEquals(42L, instance.invokeMember("val").asLong());
    }

    @Test
    @Disabled("TODO: implemnent meta field access, then re-enable this test.")
    void testMetaFieldAccess() {
        String source = """
            class MyClass {
                meta Long X = 42; 
                meta Long val() { ^ X }
                Long val() { ^ X }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // field access in method
        assertEquals(42L, instance.invokeMember("val").asLong());
        //meta method
        assertEquals(42L, instance.invokeMember("val").asLong());
    }

}
