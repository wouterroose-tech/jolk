package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
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
    void testFieldAccessors() {
        String source = "class MyClass { Long x;}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // access via synthesized accessor
        assertEquals(0L, instance.invokeMember("x").asLong());
        assertEquals(instance, instance.invokeMember("x", 42L));
        assertEquals(42L, instance.invokeMember("x").asLong());
    }

    @Test
    void testFieldAccess() {
        String source = "class MyClass { Long x = 42; Long val() { ^ self #x } }";
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
    void testMetaFieldAccess() {
        String source = """
            class MyClass {
                meta Long X = 0; 
                meta Long val() { ^ X }
                Long val() { ^ X }
                meta Self val(Long x) { ^ self #X(x)}
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        assertEquals(0L, meta.invokeMember("X").asLong());
        assertEquals(0L, meta.invokeMember("val").asLong());
        assertEquals(0L, instance.invokeMember("val").asLong());
        assertEquals(meta, meta.invokeMember("val", 42L)); 
        assertEquals(42L, meta.invokeMember("X").asLong());
        assertEquals(42L, meta.invokeMember("val").asLong());
        assertEquals(42L, instance.invokeMember("val").asLong());
    }

}
