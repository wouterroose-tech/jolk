package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkClosureTest extends JolcTestBase {

    @Test
    void testClosureNoArgs() {
        String source = """
            class MyClass {
                Long x;
                // TODO: what is the advise on the implementation of closure to support this:
                apply(Closure closure) { closure #apply() }
                run(Long v) { self #apply [ self #x(v) ] }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        instance.invokeMember("run", 42); 
        assertEquals(42, instance.invokeMember("x").asLong()); 
    }  

    @Test
    void testClosureOneArg() {
        String source = """
            class MyClass {
                Long x;
                // TODO: what is the advise on the implementation of closure to support this:
                apply(Closure closure) { closure #apply(42) }
                run() { self #apply [ Long v -> self #x(v) ] }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        instance.invokeMember("run"); 
        assertEquals(42, instance.invokeMember("x").asLong()); 
    }  

    @Test
    void testClosureVarArg() {
        String source = """
            class MyClass {
                Long x;
                // TODO: what is the advise on the implementation of closure to support this:
                apply(Closure closure) { closure #apply(40, 2) }
                run() { self #apply [ Long a, Long b -> self #x(a + b) ] }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        instance.invokeMember("run"); 
        assertEquals(42, instance.invokeMember("x").asLong()); 
    }  

}
