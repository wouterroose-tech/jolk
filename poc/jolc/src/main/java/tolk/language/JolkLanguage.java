package tolk.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import tolk.nodes.JolkRootNode;

@TruffleLanguage.Registration(
    id = JolkLanguage.ID,
    name = "Jolk",
    defaultMimeType = JolkLanguage.MIME_TYPE,
    characterMimeTypes = JolkLanguage.MIME_TYPE,
    contextPolicy = ContextPolicy.SHARED
)
public final class JolkLanguage extends TruffleLanguage<JolkContext> {
    public static final String ID = "jolk";
    public static final String MIME_TYPE = "application/x-jolk";

    @Override
    protected JolkContext createContext(Env env) {
        JolkContext context = new JolkContext(this, env);
        env.exportSymbol("jolkContext", env.asGuestValue(context));
        return context;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        // For now, we return a root node that does nothing but returns a Jolk `null`.
        // This is the minimal implementation to make evaluation work.
        JolkRootNode rootNode = new JolkRootNode(this);
        return rootNode.getCallTarget();
    }
}