package demo.validation;

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
                    self #delegate(supplier #apply(subject), executionContext);
                }
                private delegate(List<R> children, E executionContext) {
                    children #isEmpty [^self];
                    [ StructuredTaskScope #open(Joiner #allSuccessfulOrThrow) ]
                        #try [ scope ->
                            children #forEach [child -> scope #fork [validation #accept(child, executionContext) ] ];
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
                    self #master #add(ChildValidation<T, R> #new(supplier, validation))
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
                    self #supplier #apply(subject) #ifPresent [ child -> validation #accept(child, executionContext) ]
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
                    ^ ChildRequirementBridge<T, R> #new(this, supplier)
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

    @Test
    void testParsing() {
        this.childrenValidation();
        this.childRequirement();
        this.childRequirementBridge();
        this.childValidation();
        this.constraint();
        this.node();
        this.validation();
        this.validationSuite();
    }

}
