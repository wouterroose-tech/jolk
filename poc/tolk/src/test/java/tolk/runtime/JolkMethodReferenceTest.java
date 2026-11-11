package tolk.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkMethodReferenceTest extends JolcTestBase {
    
    @Test
    void testJavaInstanceMethodReference() {
        String source = """
            + java.util.function.Function;
            class Test {
                String run() {
                    ^ #call(String ##toUpperCase)
                }
                String call(Function[String, String] func) {
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
                    ^ #call(String ##valueOf)
                }
                String call(Function[Class, Integer] func) {
                    ^ func #apply(123)
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("123", instance.invokeMember("run").asString()); 
    } 
    
    @Test
    void testJolkInstanceMethodReference_self() {
        String source = """
            + java.util.function.Supplier;
            class Test {
                String run() {
                    ^ #call(self ##doRun)
                }
                String doRun() {
                    ^ "called"
                }
                String call(Supplier[String] supplier) {
                    ^ supplier #get + " from call"
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("called from call", instance.invokeMember("run").asString()); 
    }  
    
    @Test
    void testJolkInstanceMethodReference_Self() {
        String source = """
            + java.util.function.Supplier;
            class Test {
                String run() {
                    ^ #call(Self ##doRun) // This is an unbound reference, expects receiver as first arg
                }
                String doRun() {
                    ^ "called"
                }
                String call(Function[Test, String] func) { // Change to Function
                    ^ func #apply(self) + " from call" // Pass 'self' as the receiver
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("called from call", instance.invokeMember("run").asString()); 
    }
    
    @Test
    void testJolkInstanceMethodReference_synthesizedMethod() {
        String source = """
            + java.util.function.Supplier;
            class Test {
                Long x;
                Long get() {
                    ^ #call(Self ##x)
                }
                Long call(Function[Test, String] func) {
                    #x(42);
                    ^ func #apply(self) 
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("get").asInt()); 
    }
    
    @Test
    void testJolkInstanceMethodReference_type() {
        String source = """
            + java.util.function.Supplier;
            class Test {
                Long x;
                Long get() {
                    ^ #call(Test ##x)
                }
                Long call(Function[Test, String] func) {
                    #x(42);
                    ^ func #apply(self) 
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("get").asInt()); 
    }
    
    @Test
    void testJolkMetaMethodReference_type() {
        String source = """
            + java.util.function.Supplier;
            class Test {
                Long get() {
                    ^ #call(Test ##x)
                }
                Long call(Function[Test, String] func) {
                    ^ func #apply(self) 
                }
                meta Long x(Test x) { ^ 42 }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("get").asInt()); 
    }

    @Test
    void testJolkMethodReference_SynthesizedGetterField() {
        String source = """
            class Person {
                String name;
            }""";
        eval(source);
        source = """
            + java.util.function.Function;
            class Test {
                Person person;

                String getPerson(Function[Test, Person] func) {
                    ^func #apply(self) #name;
                }

                String run() {
                    Person p = Person #new #name("X");
                    #person(p);
                    ^ self #getPerson(Test ##person)
                }
            }
            """;
        Value test = eval(source).invokeMember("new");
        assertEquals("X", test.invokeMember("run").asString());
    }

    @Test
    void testJolkMethodReferenceField() {
        String source = """
            class Person {
                String name;
            }""";
        eval(source);
        source = """
            + java.util.function.Function;
            class PersonFunction {
                Person person;
                Function[Person, String] func;

                String apply() {
                    ^ func #apply(#person)
                }
            }
            """;
        eval(source);
        source = """
            class Test {
                String run() {
                    Person p = Person #new #name("X");
                    PersonFunction f = PersonFunction #new(p,  Person ##name);
                    ^ f #apply
                }
            }
            """;

        Value test = eval(source).invokeMember("new");
        assertEquals("X", test.invokeMember("run").asString());
    }
    
    @Test
    void testJolkMethodReference_new() {
        String source = """
            + java.util.function.Supplier;
            class Test {
                String run() {
                    ^ #call(Self ##new)
                }
                String call(Supplier[Test] supplier) {
                    ^ supplier #get + " from call"
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("instance of Test from call", instance.invokeMember("run").toString()); 
    }  

    /// ## testJolkMethodReference_interop
    /// 
    /// verifies host/guest interop for unbound Jolk method references
    /// Specifically ensures a type-level method reference (e.g. Test ##reduce) used as a host BinaryOperator
    /// is bound to the correct receiver (lexical self) so host call-shape (accumulator, element) doesn't
    /// become misinterpreted as the guest receiver, avoiding dispatch-on-TruffleString errors.
    /// 
    @Disabled ("TODO: fix corner case for inerop in JolKVisitor#visitMethod_reference")
    @Test
    void testJolkMethodReference_type_interop() {
        String source = """
            + java.util.ArrayList;
            class Test {
                String run() {
                    ArrayList[String] colors = Array #new("red", "green", "blue");
                    ^ colors #stream #reduce("", Test ##do)
                }
                String do(String reduced, String reducing) {
                    ^ reduced + reducing
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("redgreenblue", instance.invokeMember("run").toString());
    }

    @Test
    void testJolkMethodReference_self_interop() {
        String source = """
            + java.util.ArrayList;
            class Test {
                String run() {
                    ArrayList[String] colors = Array #new("red", "green", "blue");
                    ^ colors #stream #reduce("", self ##do)
                }
                String do(String reduced, String reducing) {
                    ^ reduced + reducing
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("redgreenblue", instance.invokeMember("run").toString());
    }

}
