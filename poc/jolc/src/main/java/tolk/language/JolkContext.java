package tolk.language;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.TruffleObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JolkContext {

    private final JolkLanguage language;
    private final TruffleLanguage.Env env;
    private final Map<String, TruffleObject> typeRegistry;
    private final Map<String, TruffleObject> javaTypeCache;

    public JolkContext(JolkLanguage language, TruffleLanguage.Env env) {
        this.language = language;
        this.env = env;
        this.typeRegistry = new ConcurrentHashMap<>();
        this.javaTypeCache = new ConcurrentHashMap<>();
    }

    public TruffleLanguage.Env getEnv() {
        return env;
    }

    /// @return The registry for Jolk-defined types.
    public Map<String, TruffleObject> getTypeRegistry() {
        return typeRegistry;
    }

    /// @return The cache for Java types that have been resolved and wrapped for Jolk.
    public Map<String, TruffleObject> getJavaTypeCache() {
        return javaTypeCache;
    }
}