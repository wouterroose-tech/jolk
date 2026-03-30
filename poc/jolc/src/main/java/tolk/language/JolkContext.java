package tolk.language;

import com.oracle.truffle.api.TruffleLanguage;
import tolk.runtime.JolkMetaClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// ## JolkContext
///
/// Represents the execution context for a Jolk program. It holds global state
/// such as defined classes and provides access to the Truffle environment.

public class JolkContext {
    private final TruffleLanguage.Env env;
    private final Map<String, JolkMetaClass> definedClasses = new ConcurrentHashMap<>();

    public JolkContext(JolkLanguage language, TruffleLanguage.Env env) {
        this.env = env;
    }

    public void registerClass(JolkMetaClass metaClass) {
        definedClasses.put(metaClass.name, metaClass);
        env.exportSymbol(metaClass.name, metaClass); // Export to polyglot environment
    }

    public JolkMetaClass getDefinedClass(String name) {
        return definedClasses.get(name);
    }
}