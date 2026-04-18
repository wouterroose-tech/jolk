package demo.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
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

    private Value executionContext() {
        String source = """
	        & demo.validation.Issue.ERROR;
            class ExecutionContext {
                constant Array<Issue> issues = Array #new;
                Self add(Object subject, Issue issue) {
                    self #issues #add(issue)
                }
                Boolean hasIssue() {
                    ^ !self #issues #isEmpty
                }
                Boolean hasError() {
                    ^ self #hasMatch [i -> i #match(ERROR) ]
                }
                Boolean hasIssue(Object subject) {
                    ^ self #hasMatch [i -> i #concerns(subject)]
                }
                private Boolean hasMatch(Predicate<Issue> p) {
                    ^ issues #anyMatch(p)
                }
            }""";  
        return eval(source);
    }

    @Test
    void testParsing() {
        this.issueType();
        this.levelEnum();
        this.executionContext();
    }

    @Test
    @Disabled("activate when Record is implemented")
    void testIssue() {
        Value level = this.levelEnum().getMember("ERROR");
        Value issue = this.issueType().invokeMember("new", null, "Test message", level);
        assertEquals(true, issue.invokeMember("isError"));
    }

}
