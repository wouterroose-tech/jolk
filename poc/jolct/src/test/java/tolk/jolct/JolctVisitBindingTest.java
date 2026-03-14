package tolk.jolct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkParser.BindingContext;

public class JolctVisitBindingTest extends JolctVisitorTest {
    

    void assertBinding(String expected, String source) {
        JolkContext context = new JolkContext();
        JolctVisitor visitor = new JolctVisitor(context);
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        BindingContext bindingContext = parser.binding();
        String result = visitor.visitBinding(bindingContext);
        assertEquals(expected, result);
    }

    @Test
    public void testBinding() {
        String source = "x = v";
        String expected = "x = v";
        assertBinding(expected, source);
    }

    @Test
    public void testBindingWithExpression() {
        String source = "x = x + 1";
        String expected = "x = x + 1";
        assertBinding(expected, source);
    }

    @Test
    void testNullCoalescing() {
        String source = "title = s ?? \"Default\"";
        String expected = "title = (s != null ? s : \"Default\")";
        assertBinding(expected, source);
    }
}