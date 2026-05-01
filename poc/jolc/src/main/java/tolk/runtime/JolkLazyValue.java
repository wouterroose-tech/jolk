package tolk.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import tolk.language.JolkLanguage;
import tolk.nodes.JolkNode;
import tolk.nodes.JolkRootNode;
import tolk.language.JolkSemanticException;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a lazily initialized value in Jolk. The initializer is executed
 * only once upon first access, and the result is cached.
 * This class ensures thread-safe initialization.
 */
public final class JolkLazyValue {

    private final JolkNode initializer;
    private final String name;
    private final JolkLanguage language;

    // AtomicReference to store the initialized value and its state.
    // It will hold either a sentinel (e.g., this itself) or the actual value.
    private final AtomicReference<Object> valueRef = new AtomicReference<>(this); // Use 'this' as a sentinel for uninitialized

    // ThreadLocal to detect circular dependencies during initialization.
    private static final ThreadLocal<Boolean> initializing = ThreadLocal.withInitial(() -> false);

    public JolkLazyValue(JolkNode initializer, String name, JolkLanguage language) {
        this.initializer = initializer;
        this.name = name;
        this.language = language;
    }

    /**
     * Returns the lazily initialized value. The initializer is executed only once
     * upon the first call to this method. Subsequent calls return the cached value.
     * This method is thread-safe.
     *
     * @param receiver The object context (self) for the initializer.
     * @return The initialized value.
     * @throws JolkSemanticException if a circular dependency is detected during initialization.
     */
    @TruffleBoundary
    public Object get(Object receiver) {
        Object result = valueRef.get();
        if (result == this) { // Check if it's the sentinel (uninitialized)
            CompilerDirectives.transferToInterpreterAndInvalidate();
            synchronized (this) {
                result = valueRef.get();
                if (result == this) { // Double-check locking
                    if (initializing.get()) {
                            int line = (initializer.getSourceSection() != null) ? initializer.getSourceSection().getStartLine() : -1;
                            throw new JolkSemanticException("Circular dependency detected during lazy initialization of '" + name + "'", line);
                    }
                    try {
                        initializing.set(true);
                        JolkRootNode root = new JolkRootNode(language, initializer, name + "_lazy_initializer");
                            result = root.getCallTarget().call(receiver);
                        valueRef.set(result); // Cache the result
                    } finally {
                        initializing.set(false);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns true if the lazy value has already been initialized.
     */
    public boolean isInitialized() {
        return valueRef.get() != this;
    }

    /**
     * Returns the name of the lazy value (for debugging/error messages).
     */
    public String getName() {
        return name;
    }
}
