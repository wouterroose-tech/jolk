package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import tolk.JolcTestBase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JolkSelectorTest  extends JolcTestBase {

    @Test
    void testCreateSelector() {
        eval(""); // Initialize context
        JolkSelector selector = JolkSelector.create("testSelector");
        assertNotNull(selector);
        assertEquals("testSelector", selector.asString());
    }

    @Test
    void testSelectorIsTruffleObject() {
        eval(""); // Initialize context
        JolkSelector selector = JolkSelector.create("anotherSelector");
        InteropLibrary interop = InteropLibrary.getUncached();

        assertTrue(interop.isString(selector));
        try {
            assertEquals("anotherSelector", interop.asString(selector));
        } catch (UnsupportedMessageException e) {
            fail("JolkSelector should support asString() interop message: " + e.getMessage());
        }
    }

    @Test
    void testSelectorEquality() {
        eval(""); // Initialize context
        JolkSelector selector1 = JolkSelector.create("equalSelector");
        JolkSelector selector2 = JolkSelector.create("equalSelector");
        JolkSelector selector3 = JolkSelector.create("differentSelector");

        assertEquals(selector1, selector2, "Selectors with the same string should be equal");
        assertNotEquals(selector1, selector3, "Selectors with different strings should not be equal");
        assertEquals(selector1.hashCode(), selector2.hashCode(), "Equal selectors should have the same hash code");
    }

    @Test
    void testSelectorToString() {
        eval(""); // Initialize context
        JolkSelector selector = JolkSelector.create("toStringTest");
        assertEquals("toStringTest", selector.toString());
    }
}
