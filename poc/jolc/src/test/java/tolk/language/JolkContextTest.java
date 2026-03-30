package tolk.language;

import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;
import tolk.runtime.JolkMetaClass;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Disabled;

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
    @Disabled
    void testContextInitialize() {
        JolkContext jolkContext = getJolkContext();
        assertNotNull(jolkContext, "The JolkContext should be accessible from the polyglot context.");
    }

    @Test
    @Disabled
    void testRegisterClass() {
        JolkContext jolkContext = getJolkContext();
        jolkContext.registerClass(new JolkMetaClass("Test", null, null, null, null));
        assertNotNull(jolkContext, "The JolkContext should be accessible from the polyglot context.");

        assertNotNull(jolkContext.getDefinedClass("Test "));
    }

    @Test
    void testContextClose() {
        context.close();
        // After closing, operations should fail with an IllegalStateException
        assertThrows(IllegalStateException.class, () -> eval("1"), "Evaluating code on a closed context should throw an exception.");
    }

}
