package tolk.jolct;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JolkTranspiler {

    public void analyze(Path sourceFile, JolkContext context) throws IOException {
        String source = Files.readString(sourceFile);
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        jolkParser parser = new jolkParser(new CommonTokenStream(lexer));
        new JolkSymbolVisitor(context).visit(parser.unit());
    }

    public String transpile(Path sourceFile, JolkContext context) throws IOException {
        String source = Files.readString(sourceFile);
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        jolkParser parser = new jolkParser(new CommonTokenStream(lexer));
        String javaCode = new JolctVisitor(context).visit(parser.unit());
        try {
            return new Formatter().formatSource(javaCode);
        } catch (Throwable e) {
            return javaCode;
        }
    }
}