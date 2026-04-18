package tolk.language;

/**
 * Custom exception for reporting Jolk syntax errors during parsing.
 */
public class JolkSyntaxException extends Exception {
    public JolkSyntaxException(String message) {
        super(message);
    }
    public JolkSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }
}