package tolk.language;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

///
/// Verifies the behavior of the Jolk execution context. These tests ensure that
/// state is correctly maintained across multiple evaluations within the same
/// context and that separate contexts remain isolated from each other.
///
public class JolkContextTest extends JolcTestBase {

    private JolkContext getJolkContext() {
        context.initialize(JolkLanguage.ID);
        return context.getPolyglotBindings()
                .getMember("jolkContext")
                .as(JolkContext.class);
    }

    @Test
    void testContextInitializesWithEmptyRegistriesAndBindings() {
        JolkContext jolkContext = getJolkContext();
        assertNotNull(jolkContext, "The JolkContext should be accessible from the polyglot context.");

        assertNotNull(jolkContext.getEnv(), "Truffle Env should be available.");
        assertNotNull(jolkContext.getTypeRegistry(), "Type registry should be initialized.");
        assertNotNull(jolkContext.getJavaTypeCache(), "Java type cache should be initialized.");
        assertNotNull(jolkContext.getTopLevelBindings(), "Top-level bindings should be initialized.");
    }

    @Test
    @Disabled("Pending implementation of variable bindings in JolkContext.")
    void testVariablePersistenceAcrossEvaluations() {
        // Define a variable in the first evaluation
        eval("x = 42");

        // Access the variable in a subsequent evaluation
        Value result = eval("x");

        assertEquals(42, result.asInt(), "Variables defined in one evaluation should be accessible in subsequent evaluations within the same context.");
    }

    @Test
    @Disabled("Pending implementation of variable bindings in JolkContext.")
    void testContextIsolation() {
        // Define 'x' in the default context (from setUp)
        eval("x = 100");

        // Create a separate, new context
        try (Context secondContext = Context.create(JolkLanguage.ID)) {
            // Define x in the second context to a different value
            secondContext.eval(JolkLanguage.ID, "x = 200");

            Value resultSecond = secondContext.eval(JolkLanguage.ID, "x");
            assertEquals(200, resultSecond.asInt(), "The second context should have its own value for 'x'.");

            // Check original context is untouched
            Value resultFirst = eval("x");
            assertEquals(100, resultFirst.asInt(), "Contexts must be isolated; changes in one should not affect the other.");
        }
    }

    @Test
    void testContextClose() {
        context.close();
        // After closing, operations should fail with an IllegalStateException
        assertThrows(IllegalStateException.class, () -> eval("1"), "Evaluating code on a closed context should throw an exception.");
    }

}
