package tolk.jolct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;

public abstract class JolctVisitorTest {

    protected String transpile(String source) {
        return transpile(source, new JolkContext());
    }

    protected String transpile(String source, JolkContext context) {
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        JolctVisitor visitor = new JolctVisitor(context);
        return visitor.visit(parser.unit());
    }

    protected void assertFullTranspilation(String expected, String source, JolkContext context) {
        String result = transpile(source, context);
        assertEquals(expected.replace("\r\n", "\n").trim(), result.replace("\r\n", "\n").trim());
    }

    protected void assertFullTranspilation(String expected, String source) {
        assertFullTranspilation(expected,source, new JolkContext());
    }
}