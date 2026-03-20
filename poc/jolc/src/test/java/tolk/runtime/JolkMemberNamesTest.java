package tolk.runtime;

import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JolkMemberNamesTest {

    @Test
    void testEmptyMemberNames() {
        JolkMemberNames names = new JolkMemberNames(new String[]{});
        assertTrue(names.hasArrayElements());
        assertEquals(0, names.getArraySize());
        assertFalse(names.isArrayElementReadable(0));
    }

    @Test
    void testMemberNamesAccess() throws InvalidArrayIndexException {
        String[] data = {"foo", "bar", "baz"};
        JolkMemberNames names = new JolkMemberNames(data);
        
        assertTrue(names.hasArrayElements());
        assertEquals(3, names.getArraySize());
        
        assertTrue(names.isArrayElementReadable(0));
        assertTrue(names.isArrayElementReadable(2));
        assertFalse(names.isArrayElementReadable(3));
        assertFalse(names.isArrayElementReadable(-1));

        assertEquals("foo", names.readArrayElement(0));
        assertEquals("bar", names.readArrayElement(1));
        assertEquals("baz", names.readArrayElement(2));
    }

    @Test
    void testInvalidAccessThrowsException() {
        JolkMemberNames names = new JolkMemberNames(new String[]{"one"});
        assertThrows(InvalidArrayIndexException.class, () -> names.readArrayElement(1));
        assertThrows(InvalidArrayIndexException.class, () -> names.readArrayElement(-1));
    }
}
