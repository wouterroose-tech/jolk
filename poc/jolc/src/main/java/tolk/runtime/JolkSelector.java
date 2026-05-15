package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * ### JolkSelector
 * 
 * Represents a reified communicative identity (a message name) within the 
 * JoMoo kernel. It acts as the "Atomic Identity" that bridges nominal strings 
 * to active message sends.
 * 
 * Adhering to the principle of **Identity Congruence**, selectors are interned 
 * to ensure that identical names map to the same physical identity in the 
 * communicative field.
 */
@ExportLibrary(InteropLibrary.class)
public final class JolkSelector implements TruffleObject {

    private static final Map<String, JolkSelector> CACHE = new ConcurrentHashMap<>();

    private final String name;

    private JolkSelector(String name) {
        this.name = name.intern();
    }

    /**
     * ### create
     * 
     * Returns the atomic identity for the given message name.
     */
    @TruffleBoundary
    public static JolkSelector create(Object name) {
        String s = String.valueOf(name);
        return CACHE.computeIfAbsent(s, JolkSelector::new);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @ExportMessage
    public boolean isString() {
        return true;
    }

    @ExportMessage
    public String asString() {
        return name;
    }

    @ExportMessage
    public String toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return "#" + name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof JolkSelector other && name.equals(other.name));
    }
}