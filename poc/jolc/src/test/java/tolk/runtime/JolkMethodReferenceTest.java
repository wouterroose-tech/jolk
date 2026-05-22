package tolk.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkMethodReferenceTest extends JolcTestBase {
    
    @Test
    void testJavaInstanceMethodReference() {
        String source = """
            + java.util.function.Function;
            class Test {
                String run() {
                    ^ self #call(String ##toUpperCase)
                }
                String call(Function<String, String> func) {
                    ^ func #apply("called")
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("CALLED", instance.invokeMember("run").asString()); 
    } 
    
    @Test
    void testJavaClassMethodReference() {
        String source = """
            + java.util.function.Function;
            class Test {
                String run() {
                    ^ self #call(String ##valueOf)
                }
                String call(Function<Class, Integer> func) {
                    ^ func #apply(123)
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("123", instance.invokeMember("run").asString()); 
    } 
    
    @Test
    void testJolkMethodReference_self() {
        String source = """
            + java.util.function.Supplier;
            class Test {
                String run() {
                    ^ self #call(self ##doRun)
                }
                String doRun() {
                    ^ "called"
                }
                String call(Supplier<String> supplier) {
                    ^ supplier #get + " from call"
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("called from call", instance.invokeMember("run").asString()); 
    }  
    
    @Test
    void testJolkMethodReference_Self() {
        String source = """
            + java.util.function.Supplier;
            class Test {
                String run() {
                    ^ self #call(Self ##doRun) // This is an unbound reference, expects receiver as first arg
                }
                String doRun() {
                    ^ "called"
                }
                String call(Function<Test, String> func) { // Change to Function
                    ^ func #apply(self) + " from call" // Pass 'self' as the receiver
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("called from call", instance.invokeMember("run").asString()); 
    }  
    
    @Test
    void testJolkMethodReference_new() {
        String source = """
            + java.util.function.Supplier;
            class Test {
                String run() {
                    ^ self #call(Self ##new)
                }
                String call(Supplier<Test> supplier) {
                    ^ supplier #get + " from call"
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("instance of Test from call", instance.invokeMember("run").toString()); 
    }  

}
