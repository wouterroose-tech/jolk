package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

public class JolkExceptionTest extends JolcTestBase {

    @Test
    void testJolkExceptionCreation() {
        String myClass = """
            + java.lang.RuntimeException;
            class MyClass { Object interrupt() { ^ RuntimeException #new }
            }""";
        
        Value instance = eval(myClass).invokeMember("new");
        assertNotNull(instance.invokeMember("interrupt"));
        Value exception = instance.invokeMember("interrupt");
        assertEquals("RuntimeException", exception.asHostObject().getClass().getSimpleName());
    }

    @Test
    void testJolkExceptionExtension() {
        String myClass = """
            + java.lang.RuntimeException;
            class MyClass { Long interrupt() { RuntimeException #new #throw } }
            """;
        
        Value instance = eval(myClass).invokeMember("new");
        assertThrows(RuntimeException.class, () -> instance.invokeMember("interrupt"));
    }
    
    @Test
    void testJolkException() {
        String interrupt = """
            + java.lang.RuntimeException;
            class Interrupt extends RuntimeException {  }
            """;
        String myClass = "class MyClass { Long interrupt() { ^ Interrupt #new #throw } }";
        
        eval(interrupt);
        Value instance = eval(myClass).invokeMember("new");
        
        assertThrows(RuntimeException.class, () -> instance.invokeMember("interrupt"));
    }

    @Test
    void testCatch() {
        String myClass = """
            + java.lang.RuntimeException;
            class MyClass {
                Long run() {
                    [ RuntimeException #new #throw ] #catch [RuntimeException e ->  ^ 42 ];
                    ^ 0
                }
        }""";
        
        Value instance = eval(myClass).invokeMember("new");
        assertEquals(42, instance.invokeMember("run").asLong());
    }

    @Test
    void testCatchFinally() {
        String myClass = """
            + java.lang.RuntimeException;
            class MyClass {
                Long run() {
                    [ RuntimeException #new #throw ]
                        #catch [RuntimeException e ->  //ignore ]
                        #finally [ ^ 42 ]
                    ^ 0
                }
            } """;
        
        Value instance = eval(myClass).invokeMember("new");
        assertEquals(42, instance.invokeMember("run").asLong());
    }

    @Test
    void testTry() {
        String myClass = """
            + java.lang.RuntimeException;
            + java.util.concurrent.StructuredTaskScope;
            class MyClass {
                Long run() {
                    [ StructuredTaskScope #open ]
                        #try [ scope -> //ignore ];
                    ^ 0
                }
            }""";

        Value instance = eval(myClass).invokeMember("new");
        assertEquals(0, instance.invokeMember("run").asLong());
    }

    @Test
    void testTryCatchChainResult() {
        String myClass = """
            + java.lang.RuntimeException;
            +  java.util.concurrent.StructuredTaskScope;
            class MyClass {
                Long run() {
                    ^ [ StructuredTaskScope #open ]
                        #try [ scope -> 10 ]
                        #catch [ RuntimeException e -> 0 ];
                    
                }
            }""";

        Value instance = eval(myClass).invokeMember("new");
        assertEquals(10, instance.invokeMember("run").asLong());
    }

    @Test
    void testTryCatchChainHandlesException() {
        String myClass = """
            + java.lang.RuntimeException;
            + java.util.concurrent.StructuredTaskScope;
            class MyClass {
                Long run() {
                    ^ [ StructuredTaskScope #open ]
                        #try [ scope -> RuntimeException #new #throw ]
                        #catch [ RuntimeException e -> 42 ];
                }
            }""";

        Value instance = eval(myClass).invokeMember("new");
        assertEquals(42, instance.invokeMember("run").asLong());
    }

    @Test
    void testTryWithResource() {
        String myClass = """
            + java.lang.Thread;
            + java.util.concurrent.StructuredTaskScope;
            class MyClass {
                Long run() {
                    [ StructuredTaskScope #open ]
                        #try [ scope ->
                            10 #times [ scope #fork [  Thread #sleep(100) ] ];
                            scope #join
                            ]; 
                    ^ 42
                }
            } """;
        Value instance = eval(myClass).invokeMember("new");
        assertEquals(42, instance.invokeMember("run").asLong());
    }
}
