package tolk.jolct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkParser.ExpressionContext;

public class JolctVisitInstanceMethodtest  extends JolctVisitorTest {
    

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

    @Test
    void testInstanceOf() {
        String source = "s #instanceOf(String)";
        String expected = "(s instanceof String)";
        assertExpression(expected, source);
    }

    @Test
    void testIsInstance() {
        String source = "String #isInstance(s)";
        String expected = "String.class.isInstance(s)";
        assertExpression(expected, source);
    }

    @Test
    void testCasting() {
        String source = "s #as(String)";
        String expected = "((String) s)";
        assertExpression(expected, source);
    }

}
