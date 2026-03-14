package tolk.jolct;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkLexer;

public class JolctVisitPrimaryTest extends JolctVisitorTest {


    public void assertReserved(String expected, String source) {
        JolkContext context = new JolkContext();
        JolctVisitor visitor = new JolctVisitor(context);
        jolkParser.PrimaryContext primaryContext ;
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        primaryContext = parser.primary();
        String result = visitor.visitPrimary(primaryContext);
        assertEquals(expected, result);
    }

    @Test
    public void testSelf() {
        assertReserved("Self", "Self");
    }

    @Test
    public void testThis() {
        assertReserved("this", "this");
    }

    @Test
    public void testSuper() {
        assertReserved("super", "super");
    }

}