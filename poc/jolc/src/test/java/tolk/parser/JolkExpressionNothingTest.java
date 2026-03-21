package tolk.parser;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.*;

/// ## JolkExpressionNothingTest
///
///
public class JolkExpressionNothingTest extends JolcTestBase {

    private Value eval(String className, String source) {
        Value result = eval(source);
        assertFalse(result.isNull());
        assertTrue(result.isMetaObject());
        assertEquals(className, result.getMetaQualifiedName());
        return result;
    }

    @Test
    void testNullIsASingletonIdentity() {
        String source = "class NullTest { Object run() { ^ null } }";
        Value meta = eval("NullTest", source);
        Value nullValue = meta.invokeMember("new").invokeMember("run");
        // JolkNothing is a first-class object that supports messaging, so it is not a polyglot null.
        assertEquals("null", nullValue.toString());
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
    void testIfPresent() {
        String source = """
            class IfEmptyTest {
                Object run() {
                    ^ null #ifPresent [ ^1 ]
                }
                Object runEmpty() {
                    ^ null #ifEmpty [ ^1 ]
                }
            }
        """;
        Value meta = eval("IfEmptyTest", source);
        Value instance = meta.invokeMember("new");

        // #ifPresent should not execute its closure for a null receiver.
        Value ifPresentResult = instance.invokeMember("run");
        assertEquals("null", ifPresentResult.toString());

        // #ifEmpty should execute its closure for a null receiver.
        Value ifEmptyResult = instance.invokeMember("runEmpty");
        assertFalse(ifEmptyResult.isNull());
        assertEquals(1, ifEmptyResult.asInt());
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
        assertEquals("null", result.toString(), "Chaining messages on null should result in null (Silent Absorption).");
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkNothing.") 
    void testNullCoalescingOperator() {
        Value result = eval("null ?? 42");
        assertEquals(42, result.asInt(), "The null-coalescing operator '??' should return the right-hand side for a null operand.");
    }

}
