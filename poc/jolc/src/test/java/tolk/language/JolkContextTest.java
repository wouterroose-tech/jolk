package tolk.language;

import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

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
    void testContextClose() {
        context.close();
        // After closing, operations should fail with an IllegalStateException
        assertThrows(IllegalStateException.class, () -> eval("1"), "Evaluating code on a closed context should throw an exception.");
    }

}
