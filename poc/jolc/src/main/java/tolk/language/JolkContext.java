package tolk.language;

import com.oracle.truffle.api.TruffleLanguage.Env;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkMetaClass.JolkMetaClassPlaceholder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the execution context for the Jolk language.
 * This class holds environment-specific information and services.
 */
public final class JolkContext {
    public final Env env; // TruffleLanguage.Env
    private final Map<String, Object> classRegistry = new ConcurrentHashMap<>();

    public JolkContext(Env env) {
        this.env = env;
    }

    public void registerClass(JolkMetaClass metaClass) {
        Object existing = classRegistry.get(metaClass.name);
        if (existing instanceof JolkMetaClassPlaceholder placeholder) {
            placeholder.updatePlaceholder(metaClass);
        }
        classRegistry.put(metaClass.name, metaClass);
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