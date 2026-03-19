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
