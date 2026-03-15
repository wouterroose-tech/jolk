package tolk.runtime;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkNothingTest
 *
 * Verifies the behavior of Jolk's `null` identity, which is a first-class singleton
 * instance of the `Nothing` type. This test ensures that `null` correctly participates
 * in message passing, adheres to the Null Object Pattern, and supports flow control.
 */
public class JolkNothingTest extends JolcTestBase {

    @Test
    void testNullIsASingletonIdentity() {
        Value nullValue = eval("null");
        assertTrue(nullValue.isNull(), "The 'null' literal should be recognized as a null polyglot value.");
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkNothing.") 
    void testNullRespondsToCoreProtocol() {
        String source = """
            #(
                "is_present" -> (null #isPresent),
                "is_empty" -> (null #isEmpty)
            )
        """;
        Value results = eval(source);
        assertFalse(results.getMember("is_present").asBoolean(), "'null #isPresent' should evaluate to false.");
        assertTrue(results.getMember("is_empty").asBoolean(), "'null #isEmpty' should evaluate to true.");
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkNothing.") 
    void testFlowControlMessages() {
        // #ifPresent should not execute its closure for a null receiver.
        Value ifPresentResult = eval("x = 1; null #ifPresent [ x = 2 ]; ^x");
        assertEquals(1, ifPresentResult.asInt(), "The #ifPresent block should not execute on null.");

        // #ifEmpty should execute its closure for a null receiver.
        Value ifEmptyResult = eval("x = 1; null #ifEmpty [ x = 2 ]; ^x");
        assertEquals(2, ifEmptyResult.asInt(), "The #ifEmpty block should execute on null.");
    }

    @Test
    void testSilentAbsorptionOfMessages() {
        // Sending an arbitrary message to 'null' should not cause a crash.
        // It should absorb the message and return 'null' itself, enabling fluid chains.
        Value result = eval("null #someRandomMessage #anotherMessage");
        assertTrue(result.isNull(), "Chaining messages on null should result in null (Silent Absorption).");
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkNothing.") 
    void testNullCoalescingOperator() {
        Value result = eval("null ?? 42");
        assertEquals(42, result.asInt(), "The null-coalescing operator '??' should return the right-hand side for a null operand.");
    }

}
