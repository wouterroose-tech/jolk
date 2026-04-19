package tolk.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.RecognitionException;
import com.oracle.truffle.api.TruffleFile;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.parser.JolkVisitor;
import tolk.nodes.JolkNode;
import tolk.nodes.JolkRootNode;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;

/**
 * <p>The Jolk TruffleLanguage implementation. This class orchestrates the parsing of Jolk
 * source code and the construction of the Truffle AST.</p>
 *
 * <p>It enforces a strict validation boundary during parsing to ensure that only syntactically
 * correct code proceeds to AST reification.</p>
 */
@TruffleLanguage.Registration(id = "jolk", name = "Jolk", defaultMimeType = "application/x-jolk",
                              characterMimeTypes = "application/x-jolk", fileTypeDetectors = JolkLanguage.JolkFileDetector.class)
public final class JolkLanguage extends TruffleLanguage<JolkContext> {
    public static final String ID = "jolk";

    private static final LanguageReference<JolkLanguage> LANGUAGE_REF = LanguageReference.create(JolkLanguage.class);
    private static final ContextReference<JolkContext> CONTEXT_REF = ContextReference.create(JolkLanguage.class);

    /**
     * A strict error listener that converts ANTLR syntax errors into immediate
     * terminal failures by throwing a {@link ParseCancellationException}.
     */
    private static final BaseErrorListener BAIL_ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    };

    /**
     * ### JolkLanguage
     * 
     * The default no-argument constructor required by the Truffle framework 
     * for language registration and instantiation via the polyglot engine.
     */
    public JolkLanguage() {
    }

    /**
     * Returns the current JolkLanguage instance.
     * @return The JolkLanguage instance.
     */
    public static JolkLanguage getLanguage() {
        return LANGUAGE_REF.get(null);
    }

    /**
     * Returns the current JolkLanguage instance for a specific node.
     */
    public static JolkLanguage getLanguage(com.oracle.truffle.api.nodes.Node node) {
        return LANGUAGE_REF.get(node);
    }

    /**
     * Returns the context reference for this language.
     */
    public ContextReference<JolkContext> getContextReference() {
        return CONTEXT_REF;
    }

    /**
     * Returns the current JolkContext.
     * @return The JolkContext instance.
     */
    public static JolkContext getContext() {
        return CONTEXT_REF.get(null);
    }

    @Override
    protected boolean isThreadAccessAllowed(Thread thread, boolean singleThreaded) {
        return true;
    }

    @Override
    protected JolkContext createContext(Env env) {
        JolkContext context = new JolkContext(this, env);
        env.exportSymbol("jolkContext", env.asGuestValue(context));
        return context;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        JolkNode programNode = parseSource(request.getSource().getCharacters().toString());
        // The FrameDescriptor would typically be built during the visitor phase for the root node.
        // For simplicity, a null FrameDescriptor is used here, assuming it's handled downstream.
        return new JolkRootNode(this, null, programNode, "program", true).getCallTarget();
    }

    /**
     * Parses the given Jolk source code and constructs the Truffle AST.
     * This method implements the strict validation boundary as described in the plan.
     *
     * @param source The Jolk source code to parse.
     * @return The root JolkNode of the Truffle AST.
     * @throws JolkSyntaxException if any syntax errors are encountered.
     */
    public JolkNode parseSource(String source) throws JolkSyntaxException {
        try {
            CharStream charStream = CharStreams.fromReader(new StringReader(source));
            jolkLexer lexer = new jolkLexer(charStream);
            lexer.removeErrorListeners();
            lexer.addErrorListener(BAIL_ERROR_LISTENER); // Reject malformed literals immediately

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            jolkParser parser = new jolkParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(BAIL_ERROR_LISTENER); // Enforce structural congruence
            parser.setErrorHandler(new BailErrorStrategy()); // Configure BailErrorStrategy

            ParseTree parseTree = parser.unit(); // Invoke the top-level rule

            // BailErrorStrategy throws ParseCancellationException on first error, so
            // getNumberOfSyntaxErrors() should ideally be 0 here if no exception was caught.
            if (parser.getNumberOfSyntaxErrors() > 0) {
                throw new JolkSyntaxException("Parsing failed with " + parser.getNumberOfSyntaxErrors() + " syntax errors.");
            }

            JolkVisitor visitor = new JolkVisitor(this);
            return visitor.visit(parseTree);

        } catch (ParseCancellationException e) {
            String msg = e.getMessage();
            if (msg == null && e.getCause() instanceof RecognitionException re) {
                Token token = re.getOffendingToken();
                String detail = re.getMessage();
                if (detail == null) {
                    detail = "unexpected token '" + (token != null ? token.getText() : "unknown") + "'";
                }
                msg = "line " + (token != null ? token.getLine() : "?") + ":" + 
                      (token != null ? token.getCharPositionInLine() : "?") + " " + detail;
            }
            throw new JolkSyntaxException("Syntax error during parsing: " + msg, e);
        } catch (IOException e) {
            throw new JolkSyntaxException("Error reading source code: " + e.getMessage(), e);
        }
    }

    /**
     * A simple file detector for Jolk source files.
     */
    public static class JolkFileDetector implements TruffleFile.FileTypeDetector {
        @Override
        public String findMimeType(TruffleFile file) {
            if (file.getName() != null && file.getName().endsWith(".jolk")) {
                return "application/x-jolk";
            }
            return null;
        }

        @Override
        public Charset findEncoding(TruffleFile file) {
            return null;
        }
    }
}