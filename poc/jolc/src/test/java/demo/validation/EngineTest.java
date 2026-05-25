package demo.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class EngineTest extends JolcTestBase {
    
    private Value childrenValidation() {
        String source = """
            package final class ChildrenValidation<T, R> extends Node<T> {
                Function<T, List<R>> supplier;
                Validation<R> validation;
                @Override
                accept(T subject, E executionContext) {
                    self #delegate(self #supplier #apply(subject), executionContext);
                }
                private delegate(List<R> children, E executionContext) {
                    children #isEmpty [^self];
                    [ StructuredTaskScope #open(Joiner #allSuccessfulOrThrow) ]
                        #try [ scope ->
                            children #forEach [child -> scope #fork [self #validation #accept(child, executionContext) ] ];
                            scope #join ]
                        #catch [ InterruptedException e ->
                            Thread #currentThread #interrupt;
                            RuntimeException #new("Validation interrupted.", e) #throw ]
                }
            }""";
        return eval(source);
    }
    
    private Value childRequirement() {
        String source = """
            protocol ChildRequirement<T, R> {
                ValidationSuite<T> add(Validation<R> validation);
            }""";
        return eval(source);
    }
    
    private Value childRequirementBridge() {
        String source = """
            package final class ChildRequirementBridge<T, R> implements ChildRequirement<T, R> {
                ValidationSuite<T> master;
                Function<T, R> supplier;
                final ValidationSuite<T> add(Validation<R> validation) {
                    // Construct the internal node and revert to master
                    self #master #add(ChildValidation<T, R> #new(self #supplier, self #validation))
                }
            }""";
        return eval(source);
    }
    
    private Value childValidation() {
        String source = """
            package final class ChildValidation<T, R> extends Node<T> {
                Function<T, R> supplier;
                Validation<R> validation;
                @Override
                accept(T subject, ExecutionContext executionContext) {
                    self #supplier #apply(subject) #ifPresent [ child -> self #validation #accept(child, executionContext) ]
                }
            }""";
        return eval(source);
    }
    
    private Value constraint() {
        String source = """
            abstract class Constraint<T> extends Validation<T> {

                @Override
                package final doAccept(T subject, ExecutionContext executionContext) {
                    (self #isValid(subject)) ? [ ^ self ];
                    executionContext #add(subject, self #getIssue(subject, executionContext));
                    self #interrupt #ifPresent [ i -> i #throw ]
                }

                /// Returns true when the constraint is violated for the given subject.
                protected abstract Boolean isValid(T subject);

                /// Creates the issue to add when the constraint fails for the given subject.
                protected abstract Issue getIssue(T subject, ExecutionContext executionContext);
            }""";
        return eval(source);
    }
    
    private Value node() {
        String source = """
            package abstract class Node<T> {

                /// Visits this node with the given subject and execution context.
                package abstract accept(T subject, ExecutionContext executionContext);
            }""";
        return eval(source);
    }
    
    private Value validation() {
        String source = """
            package abstract class Validation<T> extends Node<T> {
                protected Boolean satisfiesPreCondition(T subject, ExecutionContext executionContext) {
                    ^ true
                }
                package final accept(T subject, ExecutionContext executionContext) {
                    self #satisfiesPreCondition(subject, executionContext) ? self #doAccept(subject, executionContext)
                }
                protected Interrupt interrupt() {
                    ^ Interrupt #NO_INTERRUPT
                }
            }""";
        return eval(source);
    }
    
    private Value validationSuite() {
        String source = """
            abstract class ValidationSuite<T> extends Validation<T> {
                stable Array<Node<T>> nodes = #[];
                final add(Constraint<T> constraint) {
                    self #nodes #add(constraint)
                }
                package final <R> add(ChildValidation<T, R> suite) {
                    self #nodes #add(suite)
                }
                final <R> ChildRequirement<T, R> subject(Function<T, R> supplier) {
                    ^ ChildRequirementBridge<T, R> #new(self, self #supplier)
                }
                final validate(T subject, ExecutionContext executionContext) {
                    [ self #accept(subject, executionContext) ]
                        #catch [ Interrupt e -> /* ignore */ ]
                }
                package final doAccept(T subject, ExecutionContext executionContext) {
                    [ self #nodes #forEach [ node -> node #accept(subject, executionContext) ] ]
                        #catch [ 
                            // the further validation of this ruleset is ignored on an interrupt
                            Interrupt e ->  (e != self #interrupt) ? e #throw
                            // no action required, the containing ruleset will resume the validation
                        ]
                }
            }""";
        return eval(source);
    }

    private Value interrupt() {
        String source = """
            final class Interrupt extends RuntimeException {
                meta constant Interrupt HALT = Interrupt #new;
                meta Interrupt new() {
                    // Removes the overhead; the identity is now a lightweight flow-control signal.
                    // Disable stack trace (writableStackTrace' = false) for performance.
                    ^super #new("Validation Halt", null, false, false)
                }
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
    void test() {
        this.interrupt();
        this.childrenValidation();
        this.childRequirement();
        this.childRequirementBridge();
        this.childValidation();
        this.constraint();
        this.node();
        this.validation();
        Value ruleSuiteClass = this.validationSuite();
        Value ruleSuite = ruleSuiteClass.invokeMember("new");
        Value context = this.executionContext().invokeMember("new");
        ruleSuite.invokeMember("validate" , null, context);
        assertFalse(context.invokeMember("hasIssue").asBoolean());
    }

}
