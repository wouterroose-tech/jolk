package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;

/**
 * ## JolkExceptionTest
 *
 * Verifies the behavior of the {@link JolkException} runtime identity.
 * This ensures that exceptions behave as polite citizens in Jolk,
 * supporting both the JVM unwinding protocol and the Jolk Object protocol.
 */
@Disabled
public class JolkExceptionTest {

    @Test
    void testThrowProtocol() {
        String message = "Identity Failure";
        JolkException exception = new JolkException(message);
        
        // Verify the exception carries the message
        assertEquals(message, exception.getJolkMessage());
        
        // Verify it is a RuntimeException
        assertThrows(JolkException.class, () -> {
            InteropLibrary.getUncached().invokeMember(exception, "throw");
        }, "Sending #throw to a JolkException must trigger JVM unwinding.");
    }

    @Test
    void testMessageIntrinsic() throws Exception {
        Object payload = 404L;
        JolkException exception = new JolkException(payload);
        
        Object result = InteropLibrary.getUncached().invokeMember(exception, "message");
        assertEquals(payload, result, "The #message selector must return the original payload.");
    }

    @Test
    void testObjectProtocolAdherence() throws Exception {
        JolkException exception = new JolkException("test");
        InteropLibrary interop = InteropLibrary.getUncached();
        
        // Identity and Meta-awareness
        assertTrue(interop.hasMetaObject(exception));
        Object metaClass = interop.invokeMember(exception, "class");
        assertEquals(JolkException.EXCEPTION_TYPE, metaClass);
        
        // Flow Control
        assertTrue((Boolean) interop.invokeMember(exception, "isPresent"));
        assertFalse((Boolean) interop.invokeMember(exception, "isEmpty"));
        
        // Equivalence
        assertTrue((Boolean) interop.invokeMember(exception, "~~", exception));
    }

    @Test
    void testDisplayString() throws Exception {
        JolkException exception = new JolkException("Critical Error");
        String display = (String) InteropLibrary.getUncached().toDisplayString(exception);
        assertTrue(display.contains("Exception: Critical Error"));
    }
}
