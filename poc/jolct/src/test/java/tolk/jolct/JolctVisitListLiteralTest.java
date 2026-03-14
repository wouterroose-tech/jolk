package tolk.jolct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkParser.ExpressionContext;

public class JolctVisitListLiteralTest extends JolctVisitorTest {
    

    void assertExpression(String expected, String source) {
        JolkContext context = new JolkContext();
        JolctVisitor visitor = new JolctVisitor(context);
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        ExpressionContext expressionContext = parser.expression();
        String result = visitor.visitExpression(expressionContext);
        // parent null in test -> ternary expression generation
        assertEquals(expected, result);
    }
    

    @Test
    void testStandaloneArray() {
        String source = "#[100, 200, 300]";
        String expected = "jolk.lang.Array.of(100, 200, 300)";
        assertExpression(expected, source);
    }
    

    @Test
    void testStandaloneSet() {
        String source = "#{100, 200, 300}";
        String expected = "jolk.lang.Set.of(100, 200, 300)";
        assertExpression(expected, source);
    }
    

    @Test
    void testStandaloneMap() {
        String source = "#(100 -> \"100\", 200 -> \"200\", 300 -> \"300\")";
        String expected = "jolk.lang.Map.of(100, \"100\", 200, \"200\", 300, \"300\")";
        assertExpression(expected, source);
    }

}
