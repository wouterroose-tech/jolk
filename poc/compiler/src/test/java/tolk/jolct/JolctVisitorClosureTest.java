package tolk.jolct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkParser.ClosureContext;

public class JolctVisitorClosureTest extends JolctVisitorTest {

    private void assertClosure(String expected, String source) {
        JolkContext context = new JolkContext();
        JolctVisitor visitor = new JolctVisitor(context);
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        ClosureContext statementContext = parser.closure();
        String result = visitor.visitClosureBody(statementContext);
        assertEquals(expected, result);
    }

    @Test
    void testPatternMatching() {
        String source = """
            other #instanceOf(Person) ? [
                Person p = other #as(Person);
                p #work();
                ^true
            ]
            """;
        String expected = """
            if ((other instanceof Person)) {
            Person p = ((Person) other);
            p.work();
            return true;
            }
            """;
        assertClosure(expected, source);
    }

    @Test
    void testPatternMatching_2() {
        String source = """
            other #instanceOf(Person) ?! [ ^false ];
            Person p = other #as(Person);
            p #work();
            ^true
            """;
        String expected = """
            if (!((other instanceof Person))) {
            return false;
            }
            Person p = ((Person) other);
            p.work();
            return true;
            """;
        assertClosure(expected, source);
    }

}
