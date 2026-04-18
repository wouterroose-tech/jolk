package tolk.language;

import com.oracle.truffle.api.TruffleLanguage.Env;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkObject;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkBooleanExtension;
import tolk.runtime.JolkLongExtension;
import tolk.runtime.JolkStringExtension;
import tolk.runtime.JolkArrayExtension;
import tolk.runtime.JolkExceptionExtension;
import tolk.runtime.JolkMetaClass.JolkMetaClassPlaceholder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the execution context for the Jolk language.
 * This class holds environment-specific information and services.
 */
public final class JolkContext {
    private final JolkLanguage language;
    public final Env env; // TruffleLanguage.Env
    private final Map<String, Object> classRegistry = new ConcurrentHashMap<>();

    public JolkContext(JolkLanguage language, Env env) {
        this.language = language;
        this.env = env;

        // ### Identity Bootstrap (The "Big Bang")
        // Establish the authoritative kernel identities within the context. This 
        // fulfills the **Archetypal Rigidity** requirement by ensuring the registry 
        // contains reified identities rather than placeholders for core types.

        // 1. Universal Root: Must be registered first to anchor the hierarchy.
        registerClass(JolkObject.OBJECT_TYPE);

        // 2. Atomic First-class Identities
        registerClass(JolkNothing.NOTHING_TYPE);
        registerClass(JolkBooleanExtension.BOOLEAN_TYPE);

        // 3. Primitive & Kinetic Identities
        registerClass(JolkLongExtension.LONG_TYPE);
        registerClass(JolkStringExtension.STRING_TYPE);
        registerClass(JolkArrayExtension.ARRAY_TYPE);
        registerClass(JolkExceptionExtension.EXCEPTION_TYPE);
    }

    public void registerClass(JolkMetaClass metaClass) {
        if (metaClass == null) {
            // Guard against partial static initialization cycles
            return;
        }
        Object existing = classRegistry.get(metaClass.name);
        if (existing instanceof JolkMetaClassPlaceholder placeholder) {
            // If a placeholder exists, replace it with the actual class.
            // The placeholder's references will now point to the actual class.
            placeholder.updatePlaceholder(metaClass);
        }
        classRegistry.put(metaClass.name, metaClass);
    }

    public JolkLanguage getLanguage() {
        return language;
    }

    public Object getDefinedClass(String name) {
        return classRegistry.get(name);
    }

    public JolkMetaClass getOrCreateClass(String name) {
        Object existing = classRegistry.get(name);
        if (existing instanceof JolkMetaClass mc) {
            return mc;
        }
        if (existing != null) {
            // The name is already taken by a HostObject (Java class).
            return null;
        }
        return (JolkMetaClass) classRegistry.computeIfAbsent(name, JolkMetaClassPlaceholder::new);
    }

    ///### registerHostClass
    ///
    /// Registers a host Java class by its fully qualified name.
    ///This makes the Java class discoverable by Jolk's type resolution.
    public void registerHostClass(String path) {
        try {
            Object hostClass = env.lookupHostSymbol(path);
            if (hostClass != null) {
                classRegistry.put(path, hostClass);
                // Simple Name Registry: allow resolution via 'RuntimeException'
                String simpleName = path.substring(path.lastIndexOf('.') + 1);
                if (!classRegistry.containsKey(simpleName)) {
                    classRegistry.put(simpleName, hostClass);
                }
            }
        } catch (Exception e) {
            // Host class not found in classpath
        }
    }
}