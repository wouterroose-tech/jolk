package tolk.parser;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.*;

/// ## JolkExpressionNothingTest
///
///
public class JolkNothingTest extends JolcTestBase {

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
    void testIfPresent() {
        String source = """
            class IfEmptyTest {
                Object run() {
                    null #ifPresent [ ^1 ];
                    ^ null
                }
                Object runEmpty() {
                    null #ifEmpty [ ^1 ];
                    ^ null
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
        assertEquals(1L, ifEmptyResult.asLong());
    }

    @Test
    void testSilentAbsorptionOfMessages() {
        // Sending an arbitrary message to 'null' should not cause a crash.
        // It should absorb the message and return 'null' itself, enabling fluid chains.
        String source = """
            class HostNullTest {
                Object run(Object host) {
                    ^ null #someRandomMessage #anotherMessage
                }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        Value result = instance.invokeMember("run", (Object) null);
        assertEquals("null", result.toString(), "Chaining messages on null should result in null (Silent Absorption).");
    }

    /**
     * Verifies that raw JVM `null` values returned from host (Java) interop calls
     * are correctly "lifted" to the Jolk `null` identity (Identity Restitution).
     */
    @Test
    void testHostNullRestitution() {
        String source = """
            class HostNullTest {
                Object callGet(Object host) {
                    ^ host #get
                }
            }
            """;
        Value meta = eval("HostNullTest", source);
        Value instance = meta.invokeMember("new");

        // Pass a Java host object that returns a raw null from its 'get' method.
        java.util.concurrent.atomic.AtomicReference<Object> hostRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        Value result = instance.invokeMember("callGet", hostRef);

        assertNotNull(result, "Result wrapper should not be null.");
        assertEquals("null", result.toString(), "The restituted identity should have the string representation 'null'.");
        // Verify the archetype identity
        assertEquals("Nothing", result.invokeMember("class").getMetaSimpleName(), "Raw JVM null from host must be lifted to the Jolk 'Nothing' identity.");
    }

}
