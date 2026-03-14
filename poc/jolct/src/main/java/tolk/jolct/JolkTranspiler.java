package tolk.jolct;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;

import java.io.IOException;
import java.nio.file.Path;

public class JolkTranspiler {

    public void analyze(Path jolkFile, JolkContext context) throws IOException {
        ParseTree tree = parse(jolkFile);
        JolkSymbolVisitor visitor = new JolkSymbolVisitor(context);
        visitor.visit(tree);
    }

    public String transpile(Path jolkFile, JolkContext context) throws IOException {
        ParseTree tree = parse(jolkFile);
        JolctVisitor visitor = new JolctVisitor(context);
        return visitor.visit(tree);
    }

    private ParseTree parse(Path jolkFile) throws IOException {
        CharStream input = CharStreams.fromPath(jolkFile);
        jolkLexer lexer = new jolkLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);

        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        JolkErrorListener errorListener = new JolkErrorListener();
        parser.addErrorListener(errorListener);

        ParseTree tree = parser.unit();

        if (errorListener.hasErrors()) {
            throw new RuntimeException(errorListener.getErrorMessages());
        }
        return tree;
    }
}