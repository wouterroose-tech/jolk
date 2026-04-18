package tolk.parser;

import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;

///
/// Verifies that a visitor can correctly traverse the parse tree generated
/// from Jolk source code. This test uses a custom visitor to build an
/// S-expression representation of the Abstract Syntax Tree (AST) to validate
/// the parser's output and the visitor's traversal logic.
///
public class JolcVisitorTest extends JolcTestBase {

    @Test
    void testVisitDeclArchetype() {
        eval("class MyClass { }");
        eval("value MyValue { }");
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
        String source = "class MyClass { run() { a == b } }";
        eval(source);
        source = "class MyClass { run() { a != b } }";
        eval(source);
    }

    @Test
    void testVisitEquivalence() {
        eval("class MyClass { run() { a ~~ b } }");
        eval("class MyClass { run() { a !~ b } }");
    }

    @Test
    void testVisitTernaryOperator() {
        // ? (ifTrue)
        eval("class MyClass { run() { condition ? 1 } }");
        // ? : (ifTrue:ifFalse:)
        eval("class MyClass { run() { condition ? 1 : 0 } }");
        // ?! (ifFalse)
        eval("class MyClass { run() { condition ?! 1 } }");
        // ?! : (ifFalse:ifTrue:)
        eval("class MyClass { run() { condition ?! 1 : 0 } }");
    }

    @Test
    void testVisitNullCoalescing() {
        // ?? operator
        eval("class MyClass { run() { a ?? b } }");
        eval("class MyClass { run() { a ?? b ?? c } }");
    }

    @Test
    void testVisitLogicalAndBitwise() {
        eval("class MyClass { run() { a || b } }");  // Logic or
        eval("class MyClass { run() { a && b } }");  // Logic and
        eval("class MyClass { run() { a | b } }");   // Inclusive or
        eval("class MyClass { run() { a |! b } }");   // Exclusive or
        eval("class MyClass { run() { a & b } }");   // Bitwise and
    }

    @Test
    void testVisitAssignment() {
        String source = "class MyClass { run() { a = b } }";
        eval(source);
    }

    @Test
    void testVisitOperatorPrecedence() {
        String source = "class MyClass { run() { a + b * c } }";
        eval(source);
    }

    @Test
    void testVisitMessageSendChain() {
        String source = "class MyClass { run() { object #message(arg1) #another } }";
        eval(source);
    }

    @Test
    void testVisitClosure() {
        String source = "class MyClass { run() { [ item -> item #process ] } }";
        eval(source);
    }

    @Test
    void testVisitClass() {
        String source = "final class MyClass { Self me() { ^ self; } }";
        eval(source);
    }

    @Test
    void testVisitMethodReference() {
        eval("class MyClass { run() { obj ##method } }");
        eval("class MyClass { run() { self ##me } }");
        eval("class MyClass { run() { String ##toUpperCase } }");
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
    void testVisitNonLocalReturn() {
        String source = """
            class Search {
                Object find(Array<Int> list) {
                    list #forEach [ x -> x > 10 ? [ ^ x ] ];
                    ^ null
                }
            }
        """;
        eval(source);
    }

    @Test
    void testVisitStableField() {
        String source = """
            class StableTest {
                stable Int id;
                public stable String name = "Jolk";
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
        // Syntax implies: constant Type name = value
        String source = """
            class ConstantTest {
                constant Int PI = 314;
            }
        """;
        eval(source);
    }

    @Test
    void testVisitMetaConstant() {
        // Syntax implies: constant Type name = value
        String source = """
            class ConstantTest {
                meta constant Int PI = 314;
            }
        """;
        eval(source);
    }

    @Test
    void testVisitExtension() {
        String source = "extension MyClass on HisClass{ }";
        eval(source);
    }

    @Test
    void testVisitEnumWithConstants() {
        String source = """
            enum Color { RED; GREEN; BLUE; }
        """;
        eval(source);
    }

    @Test
    void testVisitMessage() {
        // Unary
        eval("class MyClass { run() { obj #selector } }");
        // With arguments
        eval("class MyClass { run() { obj #selector(arg1, arg2) } }");
        // Chained
        eval("class MyClass { run() { obj #one #two(arg) } }");
    }

    @Test
    void testVisitClosureVariations() {
        // Empty
        eval("class MyClass { run() { [] } }");
        // No parameters
        eval("class MyClass { run() { [ 1 + 2 ] } }");
        // Inferred parameters
        eval("class MyClass { run() { [ a, b -> a + b ] } }");
        // Typed parameters
        eval("class MyClass { run() { [ Int a, Int b -> a + b ] } }");
    }

    @Test
    void testVisitReserved() {
        eval("class MyClass { run() { self } }");  // receiver
        eval("class MyClass { run() { super } }"); // parent context
        eval("class MyClass { run() { Self } }");  // Meta-object (translated to self#class)
        eval("class MyClass { run() { true } }");  // Boolean singleton
        eval("class MyClass { run() { false } }"); // Boolean singleton
        eval("class MyClass { run() { null } }");  // Nothing singleton
    }

    @Test
    void testVisitLiteral() {
        // Numbers
        eval("class MyClass { run() { 123 } }");
        //eval("class MyClass { run() { 12.34 } }");
        // Strings & Chars
        eval("class MyClass { run() { \"String Literal\" } }");
        eval("class MyClass { run() { 'c' } }");
        // Collections
        eval("class MyClass { run() { #[1, 2, 3] } }"); // Array
        eval("class MyClass { run() { #{1, 2, 3} } }"); // Set
        eval("class MyClass { run() { #(key -> value) } }"); // Map
    }

    @Test
    @Disabled("Activate when floating-point literals are supported")
    void testVisitDouble() {
        // Numbers
        eval("class MyClass { run() { 12.34 } }");
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

    @Test
    void testVisitPackageAndImports() {
        eval("package tolk.demo;");
        eval("~ tolk.demo;");
        eval("using tolk.util.*;");
        eval("+ tolk.util.List;");
        eval("using meta jolk.lang.Math.PI;");
        eval("& jolk.lang.Math.PI;");
    }

    /**
     * Verifies that the visitor throws an error when attempting to reassign 
     * an immutable parameter, as defined in the Jolk specification.
     */
    @Test
    void testParameterImmutabilityError() {
        String source = """
            class ErrorTest {
                Void fail(Int x) {
                    x = 42; 
                }
            }
        """;
        assertThrows(RuntimeException.class, () -> eval(source), 
            "Jolk Visitor should forbid assignment to method parameters.");
    }

    @Test
    void testVisitAnnotations() {
        // Verify that annotations at different levels don't break the visitor
        eval("@Intrinsic class MyClass { }");
        eval("class Annotated { @Deprecated Int x; @OnMethod run() {} }");
    }

    @Test
    void testVisitVariadics() {
        // Verify variadic parameters (spread operator) in method signatures
        eval("class VariadicTest { log(String... args) {} }");
    }

    @Test
    void testVisitLocalDecls() {
        // Verify typed local variable declarations inside method blocks
        eval("class LocalTest { run() { Int x = 10; stable String name = \"Jolk\"; } }");
    }

    @Test
    @Disabled("Activate when lazy evaluation is implemented")
    void testVisitLazy() {
        // Verify the lazy keyword on fields and methods
        eval("class LazyTest { lazy Int x; lazy setup() {} }");
    }

    @Test
    void testVisitArithmeticOperators() {
        eval("class MyClass { run() { a + b} }");
        eval("class MyClass { run() { a + b} }");  // Addition
        eval("class MyClass { run() { a - b} }");  // Subtraction
        eval("class MyClass { run() { a * b} }");  // Multiplication
        eval("class MyClass { run() { a / b} }");  // Division
        eval("class MyClass { run() { a % b} }");  // Modulo
        eval("class MyClass { run() { 2 ** 8} }"); // Power
        eval("class MyClass { run() { x ** y ** z} }");
        eval("class MyClass { run() { a + b * c / d % e ** f} }"); // Complex precedence
    }

}
