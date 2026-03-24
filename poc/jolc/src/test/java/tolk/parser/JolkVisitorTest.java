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
    void testVisitEquivalence() {
        eval("a ~~ b");
        eval("a !~ b");
    }

    @Test
    void testVisitTernaryOperator() {
        // ? (ifTrue)
        eval("condition ? 1");
        // ? : (ifTrue:ifFalse:)
        eval("condition ? 1 : 0");
        // ?! (ifFalse)
        eval("condition ?! 1");
        // ?! : (ifFalse:ifTrue:)
        eval("condition ?! 1 : 0");
    }

    @Test
    void testVisitNullCoalescing() {
        // ?? operator
        eval("a ?? b");
        eval("a ?? b ?? c");
    }

    @Test
    void testVisitLogicalAndBitwise() {
        eval("a || b");  // Logic or
        eval("a && b");  // Logic and
        eval("a | b");   // Inclusive or
        eval("a ^ b");   // Exclusive or
        eval("a & b");   // Bitwise and
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

    @Test
    void testVisitMetaMembers() {
        String source = """
            class MetaMemberTest {
                meta Int version = 1;
                meta Void log() { }
            }
        """;
        eval(source);
    }

    @Test
    void testVisitConstant() {
        // Syntax implies: const Type name = value
        String source = """
            class ConstTest {
                const Int PI = 314;
            }
        """;
        eval(source);
    }

    @Test
    void testVisitExtension() {
        String source = "extension MyClass { }";
        eval(source);
    }

    @Test
    void testVisitMessage() {
        // Unary
        eval("obj #selector");
        // With arguments
        eval("obj #selector(arg1, arg2)");
        // Chained
        eval("obj #one #two(arg)");
    }

    @Test
    void testVisitClosureVariations() {
        // Empty
        eval("[]");
        // No parameters
        eval("[ 1 + 2 ]");
        // Inferred parameters
        eval("[ a, b -> a + b ]");
        // Typed parameters
        eval("[ Int a, Int b -> a + b ]");
    }

    @Test
    void testVisitReserved() {
        eval("self");
        eval("super");
        eval("Self");
        eval("true");
        eval("false");
        eval("null");
    }

    @Test
    void testVisitLiteral() {
        // Numbers
        eval("123");
        eval("12.34");
        // Strings & Chars
        eval("\"String Literal\"");
        eval("'c'");
        // Collections
        eval("#[1, 2, 3]"); // Array
        eval("#{1, 2, 3}"); // Set
        eval("#(key -> value)"); // Map
    }

    @Test
    void testVisitBlock() {
        String source = """
            class BlockTest { 
                Void run() { 
                    x = 1; 
                    y = 2; 
                    ^ x + y 
                } 
            }
        """;
        eval(source);
    }
}
