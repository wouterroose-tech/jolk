package tolk.parser;

import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

///
/// Verifies that a visitor can correctly traverse the parse tree generated
/// from Jolk source code. This test uses a custom visitor to build an
/// S-expression representation of the Abstract Syntax Tree (AST) to validate
/// the parser's output and the visitor's traversal logic.
///
public class JolkVisitorTest extends JolcTestBase {

    @Test
    void testVisitDeclArchetype() {
        eval("class MyClass { }");
        eval("record MyRecord { }");
        eval("enum MyEnum { }");
        eval("protocol MyProtocol { }");
    }

    @Test
    void testVisitDeclVisibility() {
        eval("class MyClass { }");
        eval("private class MyClass { }");
        eval("protected class MyClass { }");
        eval("package class MyClass { }");
        eval("public class MyClass { }");
        eval("#> class MyClass { }");
        eval("#: class MyClass { }");
        eval("#~ class MyClass { }");
        eval("#< class MyClass { }");
    }

    @Test
    void testVisitDeclVariability() {
        eval("class MyClass { }");
        eval("abstract class MyClass { }");
        eval("final class MyClass { }");
        eval("#? class MyClass { }");
        eval("#! class MyClass { }");
    }

    @Test
    void testVisitEquality() {
        String source = "a == b";
        eval(source);
        source = "a != b";
        eval(source);
    }

    @Test
    void testVisitAssignment() {
        String source = "a = b";
        eval(source);
    }

    @Test
    void testVisitOperatorPrecedence() {
        String source = "a + b * c";
        eval(source);
    }

    @Test
    void testVisitMessageSendChain() {
        String source = "object #message(arg1) #another";
        eval(source);
    }

    @Test
    void testVisitClosure() {
        String source = "[ item -> item #process ]";
        eval(source);
    }

    @Test
    void testVisitClass() {
        String source = "final class MyClass { self me() { ^ self; } }";
        eval(source);
    }
    
    @Test
    void testVisitClass_2() {
        String source = """
            class Point {
                Int x;
                Int y;

                Boolean ~~(Object other) {
                    (self == other) ? [ ^true ];
                    other #as(Point) #ifPresent [ p ->
                        ^ (self #x == p #x) && (self #y == p #y)
                    ];
                    ^ false
                }
            }
        """;
        eval(source);
    }

}
