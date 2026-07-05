package demo.validation.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class ExecutionContextTest  extends JolcTestBase {

    private Value issueType() {
        String source = """
            ~ demo.validation.engine;
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
            ~ demo.validation.engine;
            enum Level {
                ERROR; WARNING; INFO; DEBUG;
                Boolean isError() { ^ self == ERROR }
            }""";  
        return eval(source);
    }

    private Value executionContext() {
        String source = """
            ~ demo.validation.engine;
            & java.util.function.Predicate;
            & java.util.ArrayList;
            & demo.validation.engine.Level.WARNING;
            class ExecutionContext {
                stable ArrayList<Issue> issues = #[];
                Self add(Object subject, Issue issue) {
                    self #issues #add(issue)
                }
                Boolean hasIssue() {
                    ^ !self #issues #isEmpty
                }
                Boolean hasError() {
                    ^ self #hasMatch [i -> i #level #isError ]
                }
                Boolean hasWarning() {
                    ^ self #hasMatch [i -> i #match(WARNING) ]
                }
                Boolean hasIssue(Object subject) {
                    ^ self #hasMatch [i -> i #concerns(subject)]
                }
                private Boolean hasMatch(Predicate<Issue> p) {
                    ^ self #issues #anyMatch(p)
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
    void testIssue() {
        Value levelClass = this.levelEnum();
        Value issueClass = this.issueType();
        Value level = levelClass.getMember("ERROR");
        Value issue = issueClass.invokeMember("new", null, "Test message", level);
        Value context = this.executionContext().invokeMember("new");
        assertFalse(context.invokeMember("hasIssue").asBoolean());
        assertFalse(context.invokeMember("hasError").asBoolean());
        assertFalse(context.invokeMember("hasWarning").asBoolean());

        // ERROR
        context.invokeMember("add", null, issue);
        assertTrue(context.invokeMember("hasIssue").asBoolean());
        assertTrue(context.invokeMember("hasError").asBoolean());
        assertFalse(context.invokeMember("hasWarning").asBoolean());

        // WARNING
        level = levelClass.getMember("WARNING");
        issue = issueClass.invokeMember("new", null, "Test message", level);
        context.invokeMember("add", null, issue);
        assertTrue(context.invokeMember("hasWarning").asBoolean());
    }

}
