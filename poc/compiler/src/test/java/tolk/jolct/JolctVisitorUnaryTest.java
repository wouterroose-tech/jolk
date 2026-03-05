package tolk.jolct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkParser.ExpressionContext;

public class JolctVisitorUnaryTest extends JolctVisitorTest {

    private void assertUnary(String expected, String source) {
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
    void testLogicalInversion() {
        String source = "!(x == 0)";
        String expected = "!(x == 0)";
        assertUnary(expected, source);
    }

    @Test
    void testMathematicalNegation() {
        String source = "-(x + y)";
        String expected = "-(x + y)";
        assertUnary(expected, source);
    }

}
