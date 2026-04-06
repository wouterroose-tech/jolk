package tolk.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.nodes.JolkRootNode;
import tolk.parser.JolkVisitor;

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

    private static final ContextReference<JolkContext> REFERENCE = ContextReference.create(JolkLanguage.class);

    /**
     * ### getContextReference
     * 
     * Provides public access to the context reference, allowing
     * AST nodes and visitors to resolve the {@link JolkContext}.
     * 
     * @return The context reference for the Jolk language.
     */
    public ContextReference<JolkContext> getContextReference() {
        return REFERENCE;
    }

    /**
     * Returns the current Jolk context from the thread-local state.
     */
    public static JolkContext getContext() {
        return REFERENCE.get(null);
    }

    /**
     * ### getLanguage
     * 
     * Helper to resolve the Jolk language instance from a node.
     */
    public static JolkLanguage getLanguage(Node node) {
        return node.getRootNode().getLanguage(JolkLanguage.class);
    }

    @Override
    protected JolkContext createContext(Env env) {
        JolkContext context = new JolkContext(this, env);
        env.exportSymbol("jolkContext", env.asGuestValue(context));
        return context;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        // 1. Setup ANTLR Lexer and Parser
        var lexer = new jolkLexer(CharStreams.fromReader(request.getSource().getReader()));
        var parser = new jolkParser(new CommonTokenStream(lexer));

        // 2. Parse the root rule to get the CST
        var tree = parser.unit();

        // 3. Instantiate the Visitor and convert CST to AST (This was likely missing)
        var visitor = new JolkVisitor(this); // Pass the language instance
        var rootNode = visitor.visitUnit(tree);

        // 4. Wrap the AST in a RootNode and return the CallTarget
        return new JolkRootNode(this, rootNode).getCallTarget();
    }
}