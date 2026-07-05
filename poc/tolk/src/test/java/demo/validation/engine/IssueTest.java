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
        String source = """
            enum Level {
                ERROR; WARNING; INFO; DEBUG; 

                Boolean isError() { ^ self == ERROR }
                Boolean isWarning() { ^ self == WARNING }
                Boolean isInfo() { ^ self == INFO }
                Boolean isDebug() { ^ self == DEBUG }
            }""";  
        return eval(source);
    }

    private Value issueTest() {
        String source = """
            class IssueTest {
                Boolean test_getIssue() {
                    Issue issue = Issue #new(null, "INVALID",  Level #ERROR);
                    ^ issue #message == "INVALID"  && issue #level #isError
                }
            }""";  
        return eval(source);
    }

    @Test
    void testParsing() {
        this.issueType();
        this.levelEnum();
        this.issueTest();
    }

    @Test
    void testIssue() {
        Value level = this.levelEnum().getMember("ERROR");
        Value issue = this.issueType().invokeMember("new", null, "Test message", level);
        assertTrue(issue.invokeMember("match", level).asBoolean());
    }

    @Test
    public void test_getIssue() {
        this.issueType();
        this.levelEnum();
        Value testInstance = this.issueTest().invokeMember("new");
        assertTrue(testInstance.invokeMember("test_getIssue").asBoolean());
    }

}
