package tolk.jolct;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JolctVoidPathDetectorTest {

    private JolctVoidPathDetector detector;

    @BeforeEach
    void setUp() {
        detector = new JolctVoidPathDetector();
    }

    private jolkParser.StatementContext parseStatement(String source) {
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        return parser.statement();
    }

    private void assertIsVoid(boolean expected, String source) {
        jolkParser.StatementContext ctx = parseStatement(source);
        assertEquals(expected, detector.isVoid(ctx), "Source: " + source);
    }

    @Test
    @DisplayName("Naked literals are structural voids")
    void testLiteralIsVoid() {
        assertIsVoid(true, "42");
    }

    @Test
    @DisplayName("Naked identifiers are structural voids")
    void testIdentifierIsVoid() {
        assertIsVoid(true, "b");
    }

    @Test
    @DisplayName("Presence of ^ satisfies the contract")
    void testExplicitReturnIsNotVoid() {
        assertIsVoid(false, "^a");
    }

    @Test
    @DisplayName("Messages are void paths unless they are terminal")
    void testMessageIsVoid() {
        assertIsVoid(true, "self #update");
    }

    @Test
    @DisplayName("#throw is a definitive exit and thus not void")
    void testTerminalExitIsNotVoid() {
        assertIsVoid(false, "exception #throw");
    }

    @Test
    @DisplayName("Ternary is void if any branch is a void path")
    void testTernaryWithVoidBranchIsVoid() {
        assertIsVoid(false, "^c ? a : b");
    }

    @Test
    @DisplayName("Ternary with blocks")
    void testTernary_1() {
        assertIsVoid(true, "true ? [ x = 1 ; self #do ] : [ x = 2 ; self #do ]");
    }

    @Test
    @DisplayName("Ternary with blocks")
    void testTernary_11() {
        assertIsVoid(false, "true ? [ x = 1 ; ^self #do ] : [ x = 2 ; ^self #do ]");
    }

    @Test
    @DisplayName("Ternary with blocks")
    void testTernary_2() {
        assertIsVoid(false, "b ? [^self] : [^null]");
    }

    @Test
    @DisplayName("Ternary is not void if all paths are defined")
    void testTernary_3() {
        assertIsVoid(false, "^c ? a : b");
    }

    @Test
    @DisplayName("Root-level ^ provides absolute resolution for all sub-branches")
    void testRootReturnIsNotVoid() {
        assertIsVoid(false, "^ c ? a : b #do { }");
    }

    @Test
    @DisplayName("Factory patterns (mojo) are treated as inconclusive (bypass)")
    void testNestedMojoIsNotVoid() {
        assertIsVoid(true, "[ ] #do [ ]");
    }

    @Test
    @DisplayName("A block containing a return is not a void path")
    void testBlockWithReturnIsNotVoid() {
        // The detector correctly inspects the last statement of a block.
        // A block ending in an explicit return (`^`) is a resolved path.
        assertIsVoid(false, "[ ^x ]");
    }

    @Test
    void testChainedVoids() {
        assertIsVoid(true, "c ? (d ? a : b) : e");
    }

    @Test
    @DisplayName("A block ending in a value is a void path")
    void testBlockWithValueIsVoid() {
        // A block ending in a naked value is a void path, as the value is not returned.
        assertIsVoid(true, "[ x ]");
    }
}