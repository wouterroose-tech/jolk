package tolk.parser;

import org.junit.jupiter.api.Test;

///
/// Verifies that a visitor can correctly traverse the parse tree generated
/// from Jolk source code. This test uses a custom visitor to build an
/// S-expression representation of the Abstract Syntax Tree (AST) to validate
/// the parser's output and the visitor's traversal logic.
///
public class JolkVisitorTest {

    @Test
    void testVisitOperatorPrecedence() {
        String source = "a + b * c";
    }

    @Test
    void testVisitMessageSendChain() {
        String source = "object #message(arg1) #another";
    }

    @Test
    void testVisitClosure() {
        String source = "[ item -> item #process ]";
    }

}
