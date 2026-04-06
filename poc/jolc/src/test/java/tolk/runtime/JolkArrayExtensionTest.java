package tolk.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.oracle.truffle.api.interop.InteropLibrary;

import java.util.ArrayList;
import java.util.List;
import tolk.JolcTestBase;
import tolk.language.JolkLanguage;
import org.graalvm.polyglot.Value;

/**
 * ## JolkArrayExtensionTest
 *
 * Validates the runtime implementation of the Array archetype extensions.
 * Verifies that Jolk selectors like #at, #put, and #new correctly map to 
 * java.util.List operations while adhering to the Jolk messaging protocol.
 */
public class JolkArrayExtensionTest extends JolcTestBase {

    private final InteropLibrary interop = InteropLibrary.getUncached();

    /**
     * Verifies the meta-level factory method for creating native lists.
     * Jolk: ArrayList #new
     */
    @Test
    void testNew() throws Exception {
        eval("");
        context.enter();
        try {
            // Use the Polyglot context to wrap the internal Truffle result
            Object rawResult = JolkArrayExtension.ARRAY_TYPE.invokeMember("new", new Object[]{});
            Value result = context.asValue(rawResult);
            
            assertNotNull(result);
            assertTrue(result.isHostObject(), "Factory #new should create a Host Object.");
            List<?> list = result.as(List.class);
            assertEquals(0, list.size());
        } finally {
            context.leave();
        }
    }

    /**
     * Verifies the meta-level factory method for creating native lists.
     * Jolk: ArrayList #new(1, 2, 3)
     */
    @Test
    void testVariadicNew() throws Exception {
        eval("");
        context.enter();
        try {
            Object rawResult = JolkArrayExtension.ARRAY_TYPE.invokeMember("new", new Object[]{1L, 2L, 3L});
            Value result = context.asValue(rawResult);
            
            assertNotNull(result);
            assertTrue(result.isHostObject(), "Factory #new should create a Host Object.");
            List<?> list = result.as(List.class);
            assertEquals(3, list.size());
            assertEquals(1L, list.get(0));
            assertEquals(3L, list.get(2));
        } finally {
            context.leave();
        }
    }

    /**
     * Verifies positional retrieval via #at.
     * Jolk: list #at(1)
     */
    @Test
    void testAt() throws Exception {
        eval("");
        context.enter();
        try {
            List<String> list = new ArrayList<>(List.of("first", "second"));
            // Identity Restitution: Wrap raw Java object as a Guest-visible HostObject.
            Object guestList = JolkLanguage.getContext().env.asGuestValue(list);
            
            Object method = JolkArrayExtension.ARRAY_TYPE.lookupInstanceMember("at");
            
            // Wrap the result to access standard Polyglot coercion (like asString)
            Object rawResult = interop.execute(method, guestList, 1L);
            Value result = context.asValue(rawResult);
            assertEquals("second", result.asString());
        } finally {
            context.leave();
        }
    }

    /**
     * Verifies mutation and Receiver Retention.
     * Jolk: list #put(0, "new")
     */
    @Test
    void testPut() throws Exception {
        eval("");
        context.enter();
        try {
            List<Object> list = new ArrayList<>(List.of("old"));
            // Identity Restitution: Wrap raw Java object as a Guest-visible HostObject.
            Object guestList = JolkLanguage.getContext().env.asGuestValue(list);
            
            Object method = JolkArrayExtension.ARRAY_TYPE.lookupInstanceMember("put");
            
            // Jolk Protocol: execute(receiver, index, value). Returns receiver.
            Object rawResult = interop.execute(method, guestList, 0L, "new");
            Value result = context.asValue(rawResult);
            
            assertEquals("new", list.get(0));
            assertEquals(context.asValue(guestList), result, "#put should return the receiver for fluent chaining.");
        } finally {
            context.leave();
        }
    }

    @Test
    void testAtNullRestitution() throws Exception {
        eval("");
        context.enter();
        try {
            List<Object> list = new ArrayList<>();
            list.add(null);
            // Identity Restitution: Wrap raw Java object as a Guest-visible HostObject.
            Object guestList = JolkLanguage.getContext().env.asGuestValue(list);
            
            Object method = JolkArrayExtension.ARRAY_TYPE.lookupInstanceMember("at");
            
            Object rawResult = interop.execute(method, guestList, 0L);
            assertEquals(JolkNothing.INSTANCE, rawResult, "Raw null from list should be lifted to JolkNothing.");
        } finally {
            context.leave();
        }
    }
}
