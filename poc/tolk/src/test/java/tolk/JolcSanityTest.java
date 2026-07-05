package tolk;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

///
/// ## JolcSanityTest
///
/// A foundational sanity check to ensure the Truffle language infrastructure is
/// correctly configured and can be initialized without errors. This test verifies
/// the most basic contract of the Jolk language: that an empty program evaluates
/// to the `Nothing` identity.
///
public class JolcSanityTest extends JolcTestBase {

    /**
     * ### Verifies the Core Language Contract
     *
     * This test confirms several critical behaviors at once:
     * 1.  The GraalVM `Context` is successfully created by the test harness.
     * 2.  The Jolk language can be initialized and can evaluate a program (`eval("")`).
     * 3.  The language correctly adheres to the semantic rule that an empty program
     *     (the simplest possible expression) evaluates to the `Nothing` identity (`null`).
     *
     * The assertion uses both a string check (`toString()`) and a more robust behavioral
     * check (`#isPresent`) to confirm the result is the true `Nothing` object.
     */
    @Test
    public void testLanguageContextInitializesAndEvaluates() {
        assertNotNull(context, "Context should be initialized by the base class");

        Value result = eval("");
        assertEquals("null", result.toString(), "The minimal root node should return a null value");
        assertFalse(result.invokeMember("isPresent").asBoolean(), "The result should be the Nothing identity");
    }
}
