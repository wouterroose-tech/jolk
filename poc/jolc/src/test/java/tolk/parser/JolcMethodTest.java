package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolcMethodTest  extends JolcTestBase {

    @Test
    void testMethodWithParameter() {
        String source = """
            class MyClass {
                Long eval(Long x) { ^ x } 
                Long eval1(Long x) { ^ 2 + x } 
                Long eval2(Long x) { ^ x + 2 } 
                Long eval3(Long x) { ^ x == 42 ? 1 : 0 }
                Long eval4(Long x) { ^ x < 42 ? 1 : 0 }
                Long eval5(Long x) { ^ x > 42 ? 1 : 0 }
                Long eval6(Long x) { ^ x ~~ 42 ? 1 : 0 }
                Long eval7(Boolean bool) { ^ bool == true ? 1 : 0 }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        assertEquals(42L, instance.invokeMember("eval", 42L).asLong());
        assertEquals(42L, instance.invokeMember("eval1", 40L).asLong());
        assertEquals(42L, instance.invokeMember("eval2", 40L).asLong());
        assertEquals(1L, instance.invokeMember("eval3", 42L).asLong());
        assertEquals(0L, instance.invokeMember("eval3", 0L).asLong());
        assertEquals(1L, instance.invokeMember("eval4", 0L).asLong());
        assertEquals(0L, instance.invokeMember("eval4", 42L).asLong());
        assertEquals(0L, instance.invokeMember("eval5", 0L).asLong());
        assertEquals(0L, instance.invokeMember("eval5", 42L).asLong());
        assertEquals(1L, instance.invokeMember("eval6", 42L).asLong());
        assertEquals(0L, instance.invokeMember("eval6", 0L).asLong());
        assertEquals(1L, instance.invokeMember("eval7", true).asLong());
        assertEquals(0L, instance.invokeMember("eval7", false).asLong());
    }

    @Test
    void testMethodWithParameters() {
        String source = "class MyClass { Long eval(Long x, Long y) { ^ x + y } }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        assertEquals(42L, instance.invokeMember("eval", 40L, 2L).asLong());
    }

    @Test
    void testMethodWithCall() {
        String source = """
            class MyClass {
                Long eval(Long x) { ^ self #eval1(x) } 
                private Long eval1(Long x) { ^ 2 + x } 
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        assertEquals(42L, instance.invokeMember("eval", 40L).asLong());
    }

    @Test
    void testMethodWithCalls() {
        String source = """
            class MyClass {
                Long eval(Boolean switch) { ^ switch ? self #eval1(40) : self #eval2(40) } 
                private Long eval1(Long x) { ^ 2 + x } 
                private Long eval2(Long x) { ^ x + 2 } 
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        assertEquals(42L, instance.invokeMember("eval", true).asLong());
        assertEquals(42L, instance.invokeMember("eval", false).asLong());
    }

    @Test
    void testSequentialStatements() {
        String source = "final class  MyClass  { Long eval(Long val) { val #hash; val #toString; ^ val } }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        assertEquals(42L, instance.invokeMember("eval", 42L).asLong());
    }

    @Test
    void testAssignment() {
        String source = "class MyClass { Long eval(Long val) { Long x = val; x = x + 2; ^ x } }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        assertEquals(42L, instance.invokeMember("eval", 40L).asLong());
    }

    @Test
    void testRecursivelyNestedStatements() {
        String source = "class Fibonacci { Long fib(Long n) { ^ n < 2 ? 1 : self #fib(n - 1) + self #fib(n - 2) } }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        assertEquals(1L, instance.invokeMember("fib", 0L).asLong());
        assertEquals(1L, instance.invokeMember("fib", 1L).asLong());
        assertEquals(2L, instance.invokeMember("fib", 2L).asLong());
        assertEquals(3L, instance.invokeMember("fib", 3L).asLong());
        assertEquals(5L, instance.invokeMember("fib", 4L).asLong());
        assertEquals(8L, instance.invokeMember("fib", 5L).asLong());
        assertEquals(13L, instance.invokeMember("fib", 6L).asLong());
        assertEquals(21L, instance.invokeMember("fib", 7L).asLong());
        assertEquals(34L, instance.invokeMember("fib", 8L).asLong());
        assertEquals(55L, instance.invokeMember("fib", 9L).asLong());
        assertEquals(89L, instance.invokeMember("fib", 10L).asLong());
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkNothing.") 
    void testFlowControlMessages() {
        String source = """
            class MinMaxTest {
                Long ifPresentTrue() { Long x = 42; x #ifPresent [v -> x = v ]; ^x" }
                Long ifPresentFalse() { Long x = 0; null #ifPresent [v -> x = 42 ]; ^x" }
                Long ifEmptyTrue() { Long x = 42; null #ifEmpty [ x = 0 ]; ^x"}
                Long ifEmptyFalse() { Long x = 42; null #ifEmpty [ x = 0 ]; ^x"}
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");  

        assertEquals(42L, instance.invokeMember("ifPresentTrue").asLong());
        assertEquals(42L, instance.invokeMember("ifPresentFalse").asLong());
        assertEquals(0L, instance.invokeMember("ifEmptyTrue").asLong());
        assertEquals(0L, instance.invokeMember("ifEmptyFalse").asLong());
    }

}
