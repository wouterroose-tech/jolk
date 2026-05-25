package demo.validation.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class IssueTest extends JolcTestBase {

    private Value issueType() {
        String source = """
            record Issue {
                Object subject;
                String message;
                Level level;
                Boolean concerns(Object subject) { ^ self #subject == subject }
                Boolean match(Level level) { ^ self #level == level }
            }""";
        return eval(source);
    }

    private Value levelEnum() {
        String source = "enum Level { ERROR; WARNING; INFO; DEBUG; }";  
        return eval(source);
    }

    @Test
    void testParsing() {
        this.issueType();
        this.levelEnum();
    }

    @Test
    void testIssue() {
        Value level = this.levelEnum().getMember("ERROR");
        Value issue = this.issueType().invokeMember("new", null, "Test message", level);
        assertTrue(issue.invokeMember("match", level).asBoolean());
    }

}
