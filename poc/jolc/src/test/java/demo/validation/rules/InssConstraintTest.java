package demo.validation.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class InssConstraintTest  extends JolcTestBase {

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

    private Value context() {
        String source = """
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

    private Value interrupt() {
        String source = """
            ~ test;
            class Interrupt extends RuntimeException {
                meta constant Interrupt HALT = Interrupt #new;
                meta Interrupt new() {
                    ^super #new("Validation Halt", null, false, false)
                }   
            }""";
        return eval(source);
    }

    private Value validation() {
        String source = """
            package abstract class Validation<T> {
                protected Boolean satisfiesPreCondition(T subject, ExecutionContext executionContext) {
                    ^ true
                }
                @Override
                package final accept(T subject, ExecutionContext executionContext) {
                    self #satisfiesPreCondition(subject, executionContext) ? [ self #doAccept(subject, executionContext) ]
                }
                package abstract doAccept(T subject, ExecutionContext executionContext);
                protected Interrupt interrupt() {
                    ^ Interrupt #NO_INTERRUPT
                }
            }""";
        return eval(source);
    }

    private Value constraint() {
        String source = """
            abstract class Constraint<T> extends Validation<T> {
                package final doAccept(T subject, ExecutionContext executionContext) {
                    System #out #println("### reached doAccept");
                    #isValid(subject) ? [ ^ self ];
                    executionContext #add(subject, #getIssue(subject, executionContext));
                    self #interrupt #ifPresent [ i -> i #throw ]
                }
                protected abstract Boolean isValid(T subject);
                protected abstract Issue getIssue(T subject, ExecutionContext executionContext);
            }""";
        return eval(source);
    }

    private Value inssConstraint() {
        String source = """
            + demo.validation.domain.Person;
            #! class InssConstraint extends Constraint<Person> {
                #: Boolean isValid(Person person) {
                    ^ self #isValid(person #ssn)
                }
                #> Boolean isValid(Long ssn) {
                    ^ 97 - ((ssn / 100) % 97) == (ssn % 100)
                }
                #: Issue getIssue(Person person,  ExecutionContext executionContext) {
                    ^ Issue #new(person, "INSS_INVALID",  Level #ERROR)
                }
                #: Interrupt interrupt() {
                    ^ FORM_INTERRUPT
                }
            }""";
        return eval(source);
    }

    private Value person() {
        String source = """
            ~ demo.validation.domain;
            public class Person {
                Long ssn;
            }""";
        return eval(source);
    }

    private Value testInssConstraint() {
        String source = """
            + demo.validation.domain.Person;
            public class Test {
                Boolean test() {
                    ExecutionContext context = ExecutionContext #new;
                    // Use a valid SSN logic: 800101035 -> 97 - (8001010 % 97) = 35
                    Person person = Person #new #ssn(800101035);
                    InssConstraint #new #accept(person, context);
                    ^ !context #hasIssue
                }
            }""";
        return eval(source);
    }

    @Test
    void testParsing() {
        this.issueType();
        this.context();
        this.levelEnum();
        this.person();
        this.interrupt();
        this.constraint();
        this.inssConstraint();
        this.testInssConstraint();
    }

    @Test
    void test() {
        this.issueType();
        this.context();
        this.levelEnum();
        this.person();
        this.interrupt();
        this.validation();
        this.constraint();
        this.inssConstraint();
        Value test = this.testInssConstraint().invokeMember("new");
        assertTrue(test.invokeMember("test").asBoolean());
    }

}
