package tolk.language;

import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkObject;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkBooleanExtension;
import tolk.runtime.JolkLongExtension;
import tolk.runtime.JolkStringExtension;
import tolk.runtime.JolkArrayExtension;
import tolk.runtime.JolkExceptionExtension;
import tolk.runtime.JolkMetaClass.JolkMetaClassPlaceholder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final Map<String, String> projectionMappings = new HashMap<>();
    private final List<String> projectionLenses = new ArrayList<>();


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
        registerClass(JolkMessageNotUnderstoodException.TYPE);
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
    /// Registers a meta-projection lens (using meta or &).
    ///
    /// @param path The fully qualified path (e.g., java.lang.Math.PI).
    /// @param alias An optional local alias.
    /// @param isStar True if it's a star-projection (e.g., java.lang.Math.*).
    public void registerProjection(String path, String alias, boolean isStar) {
        if (isStar) {
            // Strip the ".*" if present and add to lenses
            String cleanPath = path.endsWith(".*") ? path.substring(0, path.length() - 2) : path;
            projectionLenses.add(cleanPath);
        } else if (alias != null) {
            projectionMappings.put(alias, path);
        } else {
            // Default alias is the last segment of the path (e.g., PI)
            String name = path.substring(path.lastIndexOf('.') + 1);
            projectionMappings.put(name, path);
        }
    }

    /// Resolves a projected identifier into a host or guest identity.
    ///
    /// @param name The local identifier name.
    /// @return The resolved fact (Constant, Type, or Method) or null.
    public Object lookupProjection(String name) {
        // 1. Check explicit mappings (e.g., & java.lang.Math.PI)
        String fullPath = projectionMappings.get(name);
        if (fullPath != null) {
            Object res = resolvePath(fullPath);
            if (res != null) return res;
        }

        // 2. Check lenses (star projections, e.g., & java.lang.Math.*)
        for (String lens : projectionLenses) {
            Object host = resolvePath(lens);
            if (host != null) {
                try {
                    InteropLibrary interop = InteropLibrary.getUncached(host);
                    if (interop.isMemberReadable(host, name)) {
                        return interop.readMember(host, name);
                    }
                } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                    // Try next lens
                }
            }
        }
        return null;
    }

    private Object resolvePath(String path) {
        // Try resolving as a Class first (Guest or Host)
        Object clazz = getDefinedClass(path);
        if (clazz != null) return clazz;
        try {
            return env.lookupHostSymbol(path);
        } catch (Exception e) {}

        // Try resolving as Class.Member
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            String className = path.substring(0, lastDot);
            String memberName = path.substring(lastDot + 1);
            Object base = getDefinedClass(className);
            if (base == null) {
                try {
                    base = env.lookupHostSymbol(className);
                } catch (Exception e) {}
            }
            if (base != null) {
                try {
                    return InteropLibrary.getUncached(base).readMember(base, memberName);
                } catch (Exception e) {}
            }
        }
        return null;
    }
}