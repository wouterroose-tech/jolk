package tolk.jolct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkParser.ExpressionContext;

public class JolctVisitorMessageTest  extends JolctVisitorTest {

    void assertMessage(String expected, String source) {
        JolkContext context = new JolkContext();
        JolctVisitor visitor = new JolctVisitor(context);
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        ExpressionContext expressionContext = parser.expression();
        String result = visitor.visitExpression(expressionContext);
        // parent null in test -> ternary expression generation
        assertEquals(expected, result);
        visitor.visit(parser.unit());
    }

    @Test
    void testMessage() {
        String source = "x #do";
        String expected = "x.do()";
        assertMessage(expected, source);
    }

    @Test
    void testMessag_2() {
        String source = "x #do(a)";
        String expected = "x.do(a)";
        assertMessage(expected, source);
    }

    @Test
    void testMessageChain() {
        String source = "x #do #do";
        String expected = "x.do().do()";
        assertMessage(expected, source);
    }

    @Test
    void testMessageChain_2() {
        String source = "x #do(a) #do(b)";
        String expected = "x.do(a).do(b)";
        assertMessage(expected, source);
    }

    @Test
    void testMessageChain_3() {
        String source = "x #do(a) #do";
        String expected = "x.do(a).do()";
        assertMessage(expected, source);
    }

    @Test
    void testMessageChain_4() {
        String source = "x #do #do(b)";
        String expected = "x.do().do(b)";
        assertMessage(expected, source);
    }

    @Test
    void testMessageChain_5() {
        String source = "x #do(a) #do #do(b)";
        String expected = "x.do(a).do().do(b)";
        assertMessage(expected, source);
    }

    @Test
    void testMessageChain_6() {
        String source = "x #do #do #do(b)";
        String expected = "x.do().do().do(b)";
        assertMessage(expected, source);
    }

    @Test
    void testIsPresent() {
        String source = "x #isPresent";
        String expected = "(x != null)";
        assertMessage(expected, source);
    }

    @Test
    void testIsPresent_2() {
        String source = "x #do #isPresent";
        String expected = "(x.do() != null)";
        assertMessage(expected, source);
    }

    @Test
    void testIsEmpty() {
        String source = "x #isEmpty";
        String expected = "(x == null)";
        assertMessage(expected, source);
    }

    @Test
    void testIsEmpty_2() {
        String source = "x #do #isEmpty";
        String expected = "(x.do() == null)";
        assertMessage(expected, source);
    }

    @Test
    void testIfPresent() {
        String source = "x #ifPresent [ x -> x #do ]";
        String expected = "final var _subj0 = x;\n" +
                "if (_subj0 != null) {\n" +
                "final var x = _subj0;\n" +
                "x.do();\n" +
                "}"; 
        assertMessage(expected, source);
    }

    @Test
    void testIfEmpty() {
        String source = "x #ifEmpty [ this #do ]";
        String expected = "final var _subj0 = x;\n" +
                "if (_subj0 == null) {\n" +
                "this.do();\n" +
                "}";
        assertMessage(expected, source);
    }

    @Test
    void testThrow() {
        String source = "x #throw";
        String expected = "throw x";
        assertMessage(expected, source);
    }

    @Test
    void testCatch() {
        String source = "[ x #do ] #catch [ Interrupt e -> ]";
        String expected = """
                try {
                x.do();
                } catch (Interrupt e) {
                }
                """.trim(); //trim the last new line
        assertMessage(expected, source);
    }

    @Test
    void testClosureParam() {
        String source = "self #hasMatch [i -> subject == i #subject]";
        String expected = "this.hasMatch(i -> subject == i.subject())";
        assertMessage(expected, source);
    }

    @Test
    void testClosureParam_2() {
        String source = "self #map [k, v -> x #do(v)]";
        String expected = "this.map((k, v) -> x.do(v))";
        assertMessage(expected, source);
    }

    @Test
    void testClosureTypedParam() {
        String source = "self #hasMatch [Int i -> subject == i #subject]";
        String expected = "this.hasMatch((Int i) -> subject == i.subject())";
        assertMessage(expected, source);
    }

    @Test
    void testClosureTypedParam_2() {
        String source = "self #hasMatch [Int i, Value v -> subject == i #subject]";
        String expected = "this.hasMatch((Int i, Value v) -> subject == i.subject())";
        assertMessage(expected, source);
    }
}