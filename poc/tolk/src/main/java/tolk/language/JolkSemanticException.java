package tolk.language;

/**
 * ### JolkSemanticException
 * 
 * Thrown when the Tolk Engine detects a violation of Jolk's semantic rules
 * (e.g., unreachable code, invalid assignments) during the AST reification phase.
 */
public class JolkSemanticException extends RuntimeException {
    private final int line;

    public JolkSemanticException(String message, int line) {
        super(String.format("Jolk Semantic Error [line %d]: %s", line, message));
        this.line = line;
    }

    /**
     * Returns the line number where the semantic violation occurred.
     * @return The source line number.
     */
    public int getLine() {
        return line;
    }
}