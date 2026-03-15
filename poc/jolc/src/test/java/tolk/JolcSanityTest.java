package tolk;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

///
/// A basic sanity check to ensure the Truffle language infrastructure is correctly
/// configured and can be initialized without errors.
///
public class JolcSanityTest extends JolcTestBase {

    @Test
    public void testLanguageContextInitializesAndEvaluates() {
        assertNotNull(context, "Context should be initialized by the base class");

        // With the minimal JolkRootNode, eval() should now return a value.
        Value result = eval("");
        assertTrue(result.isNull(), "The minimal root node should return a null value");
    }
}
