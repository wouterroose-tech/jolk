package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolcIteratorTest extends JolcTestBase {

    @Test
    //@Disabled("Pending implementation of Iterator") 
    void testNewArray() {
        String source = """
            class MyClass {
                ArrayList<Long> longList = ArrayList #new(); 
                ArrayList<Long> run() { ^ ArrayList<Long> #new() }    
        """;
        Value instance = eval(source);
        //assertNotNull(instance.invokeMember("longList"));
        assertNotNull(instance.invokeMember("run"));
    }

    @Test
    //@Disabled("Pending implementation of Iterator") 
    void testVariadicNewArray() {
        String source = """
            class MyClass {
                ArrayList<Long> longList = ArrayList #new(1, 2, 3);
                String run(Int key) { ^ longList #at(key) }            
        """;
        Value instance = eval(source);
        assertEquals(1, instance.invokeMember("get", 1). asInt());
    }

    @Test
    @Disabled("Pending implementation of Array Literal") 
    void testArrayLiteral() {
        String source = """
            class MyClass {
                ArrayList<Long> longList = #(1, 2, 3);
                String run(Int key) { ^ longList #at(key) }            
        """;
        Value instance = eval(source);
        assertEquals("one", instance.invokeMember("get", 1));
        assertNull(instance.invokeMember("get", 0));
    }

}
