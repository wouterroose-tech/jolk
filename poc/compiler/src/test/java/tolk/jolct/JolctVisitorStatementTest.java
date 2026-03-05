package tolk.jolct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkParser.StatementContext;

public class JolctVisitorStatementTest extends JolctVisitorTest {

    private void assertStatement(String expected, String source) {
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
    void testState() {
        String source = "constant String s = person #toString;";
        String expected = "final String s = person.toString();";
        assertStatement(expected, source);
    }

    @Test
    public void testBinding() {
        String source = "x = v;";
        String expected = "x = v;";
        assertStatement(expected, source);
    }

    @Test
    public void testBindingWithExpression() {
        String source = "x = x + 1;";
        String expected = "x = x + 1;";
        assertStatement(expected, source);
    }

    @Test
    void testTernaryAssignment() {
        String source = "x = c ? a : b;";
        String expected = "x = (c) ? a : b;";
        assertStatement(expected, source);
    }

    @Test
    void testTernaryAssignment_2() {
        String source = "x = c ?! a : b;";
        String expected = "x = (!(c)) ? a : b;";
        assertStatement(expected, source);
    }

}
