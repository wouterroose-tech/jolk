package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;
import tolk.runtime.JolkNothing;

public class JolkBindingTest extends JolcTestBase {
    
    @Test
    void testFieldInitialisation() {
        String source = "class MyClass { Long x = 42;}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // access via synthesized accessor
        assertEquals(42, instance.invokeMember("x").asLong());
    }
    
    @Test
    void testFieldAccessors() {
        String source = "class MyClass { Long x; }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // access via synthesized accessor
        assertEquals(0L, instance.invokeMember("x").asLong());
        assertEquals(instance, instance.invokeMember("x", 42));
        assertEquals(42L, instance.invokeMember("x").asLong());
    }
    
    @Test
    void testFieldGetters() {
        String source = """
            class MyClass {
                meta Long X ; 
                Long x;
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // access via synthesized accessor
        assertEquals(0, instance.invokeMember("x").asLong());
        assertEquals(0, instance.invokeMember("X").asLong());
    }

    @Test
    void testFieldAccess() {
        String source = """
            class MyClass {
                Long x = 42; 
                Long val() { ^ self #x }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // field access in method
        assertEquals(42L, instance.invokeMember("x").asLong());
        assertEquals(42L, instance.invokeMember("val").asLong());
    }

    @Test
    void testVariableBinding() {
        String source = """
            class MyClass {
                Long run(Object obj) {
                    Long x = 0; 
                    obj #ifEmpty [ x = 42 ];
                    ^x
                }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        // field access in method
        Object nothing = JolkNothing.INSTANCE;
        assertEquals(42L, instance.invokeMember("run", nothing).asLong());
        assertEquals(42L, instance.invokeMember("run", (Object) null).asLong());

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

    @Test
    void testReservedFieldName() {
        String source = """
            class MyClass {
                Long value = 42; 
                Long getValue() { ^ value }
                Long getValue(Long x) {
                    Long value = x;
                    ^ value
                }
                Long run() {
                    ^self #getValue(self #value)
                }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertEquals(42L, instance.invokeMember("value").asLong());
        assertEquals(42L, instance.invokeMember("getValue").asLong());
        assertEquals(42L, instance.invokeMember("getValue", 42).asLong());
        assertEquals(42L, instance.invokeMember("run").asLong());
    }

}
