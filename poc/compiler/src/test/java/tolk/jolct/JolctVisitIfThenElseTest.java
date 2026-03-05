package tolk.jolct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkParser.ExpressionContext;
import tolk.grammar.jolkParser.StatementContext;

public class JolctVisitIfThenElseTest extends JolctVisitorTest {
    
    void assertExpression(String expected, String source) {
        JolkContext context = new JolkContext();
        JolctVisitor visitor = new JolctVisitor(context);
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        ExpressionContext expressionContext = parser.expression();
        String result = visitor.visitExpression(expressionContext);
        assertEquals(expected, result);
    }
    
    void assertStatement(String expected, String source) {
        JolkContext context = new JolkContext();
        JolctVisitor visitor = new JolctVisitor(context);
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        StatementContext statementContext = parser.statement();
        String result = visitor.visitStatement(statementContext);
        assertEquals(expected, result);
    }

    @Test
    void testIfThenExpression() {
        String source = "x == 0 ? self #do";
        // parent null in test -> ternary expression generation
        String expected = "(x == 0) ? this.do() : null";
        assertExpression(expected, source);
    }

    @Test
    void testIfThenStatement() {
        String source = "x == 0 ? self #do";
        // parent null in test -> ternary expression generation
        String expected = "if (x == 0) {\nthis.do();\n}";
        assertStatement(expected, source);
    }

    @Test
    void testIfThenReturn() {
        String source = "^ condition ? \"True\" : \"False\"";
        // A returned ternary expression is transpiled to an if-else block.
        String expected = "if (condition) {\nreturn \"True\";\n} else {\nreturn \"False\";\n}";
        assertStatement(expected, source);
    }

    @Test
    void testIfThenAssignment() {
        String source = "y = x == 0 ? a : b;";
        String expected = "y = (x == 0) ? a : b;";
        assertStatement(expected, source);
    }

    @Test
    void testIfThenAssignment_2() {
        String source = "y = x == 0 ? a : b";
        // semicolon is optional, but should not affect the generated code
        String expected = "y = (x == 0) ? a : b;";
        assertStatement(expected, source);
    }

    @Test
    public void testReturnIfThen() {
        String source = "^b ? x : y";
        // semicolon is optional, but should not affect the generated code
        String expected = "if (b) {\nreturn x;\n} else {\nreturn y;\n}";
        assertStatement(expected, source);
    }

    @Test
    public void testReturnIfThen_2() {
        String source = "^b ?! x : y";
        // semicolon is optional, but should not affect the generated code
        String expected = "if (!(b)) {\nreturn x;\n} else {\nreturn y;\n}";
        assertStatement(expected, source);
    }

    @Test
    public void testReturnIfValueNull() {
        String source = "^b ? v : null";
        // semicolon is optional, but should not affect the generated code
        String expected = "if (b) {\nreturn v;\n} else {\nreturn null;\n}";
        assertStatement(expected, source);
    }

    @Test
    public void testReturnIfValueNull_2() {
        String source = "^b ?! v : null";
        // semicolon is optional, but should not affect the generated code
        String expected = "if (!(b)) {\nreturn v;\n} else {\nreturn null;\n}";
        assertStatement(expected, source);
    }

    @Test
    public void testReturnIfNullSelf() {
        String source = "^b ? null : v";
        // semicolon is optional, but should not affect the generated code
        String expected = "if (b) {\nreturn null;\n} else {\nreturn v;\n}";
        assertStatement(expected, source);
    }

    @Test
    void testIfThenAssignmentNullCoalescing() {
        String source = "y = x == 0 ? a;";
        // parent null in test -> ternary expression generation
        String expected = "y = (x == 0) ? a : null;";
        assertStatement(expected, source);
    }

    @Test
    public void testIfThen() {
        String source = "class Test { Self m() { x == 0 ? self #do } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self m() {\nif (x == 0) {\nthis.do();\n}\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testIfThen_2() {
        String source = "class Test { Self m() { !(x == 0) ? [^ self]; } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self m() {\nif (!(x == 0)) {\nreturn (Self) this;\n}\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testIfThen_3() {
        String source = "class Test { Self m() { x == 0 ?! [^ self]; } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self m() {\nif (!(x == 0)) {\nreturn (Self) this;\n}\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testIfThenElse() {
        String source = "class Test { Self m() { true ? [ self #do ] : [ self #do ] } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self m() {\nif (true) {\nthis.do();\n} else {\nthis.do();\n}\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testIfThenElse_2 () {
        String source = "class Test { Self m() { true ? [ x = 1 ; self #do ] : [ x = 2 ; self #do ] } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self m() {\nif (true) {\nx = 1;\nthis.do();\n} else {\nx = 2;\nthis.do();\n}\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testIfThenElse_3 () {
        String source = "class Test { Self m() { true ? [ x = 1 ] : [ x = 2 ;] } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self m() {\nif (true) {\nx = 1;\n} else {\nx = 2;\n}\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testIfNotThen() {
        String source = "class Test { Self m() { true ?! [ x = 1; ] } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self m() {\nif (!(true)) {\nx = 1;\n}\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testTernaryExpression() {
        String source = "class Test { Int x; Self m() { x = true ? 1 : 2 } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\nprivate Int x;\nprivate Int x() {\nreturn x;\n}\nprivate Self x(Int x) {\nthis.x = x;\nreturn (Self) this;\n}\npublic Self m() {\nx = (true) ? 1 : 2;\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

}