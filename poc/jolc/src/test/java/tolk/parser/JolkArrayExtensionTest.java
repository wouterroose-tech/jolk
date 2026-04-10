package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;
import tolk.runtime.JolkNothing;

public class JolkArrayExtensionTest extends JolcTestBase {

    @Test
    void testNewArray() {
        String source = """
            class MyClass {
                ArrayList<Long> longList = ArrayList #new; 
                ArrayList<Long> run() { ^ ArrayList<Long> #new }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        Value longList = instance.invokeMember("longList");
        assertNotNull(longList, "Field 'longList' should be initialized.");
        assertNotEquals(JolkNothing.INSTANCE, longList);
        assertTrue(longList.isHostObject(), "Result of ArrayList #new should be a Host Object.");
        assertEquals(ArrayList.class, longList.asHostObject().getClass());
        
        Value runValue = instance.invokeMember("run");
        assertNotNull(runValue, "Method 'run' should return a value.");
        // Verify the result is a native Java ArrayList (Shim-less Integration)
        assertTrue(runValue.isHostObject(), "Result of ArrayList #new should be a Host Object.");
        assertEquals(ArrayList.class, runValue.asHostObject().getClass());
    }

    @Test
    void testVariadicNewArray() {
        String source = """
            class MyClass {
                ArrayList<Long> longList = ArrayList #new(1, 2, 3);
                Long run(Int key) { ^ longList #at(key) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        Value longList = instance.invokeMember("longList");
        assertEquals(3, ((List<?>) longList.asHostObject()).size());
        assertNotNull(longList);
        assertEquals(ArrayList.class, longList.asHostObject().getClass());
        assertEquals(1, instance.invokeMember("run", 0).asLong());
    }

    @Test
    void testArrayLiteral() {
        String source = """
            class MyClass {
                ArrayList<Long> emptyList = #[];
                ArrayList<Long> longList = #[1, 2, 3];
                Long run(Int key) { ^ longList #at(key) }  
                Long run() { ^ longList #put(1, 42) #at(1) }          
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        Value emptyList = instance.invokeMember("emptyList");
        assertEquals(0, ((List<?>) emptyList.asHostObject()).size());
        Value longList = instance.invokeMember("longList");
        assertEquals(3, ((List<?>) longList.asHostObject()).size());
        assertEquals(1L, instance.invokeMember("run", 0).asLong()); 
        assertEquals(42, instance.invokeMember("run").asLong()); 
    }

}
