package tolk.jolct;

import org.junit.jupiter.api.Test;

import tolk.grammar.jolkParser.UnitContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import tolk.grammar.jolkParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import tolk.grammar.jolkLexer;

public class JolctImportTest extends JolctVisitorTest {

    public void assertUnit(String expected, String source, JolkContext context) {
        JolctVisitor visitor = new JolctVisitor(context);
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        UnitContext unitContext = parser.unit();
        String result = visitor.visitUnit(unitContext);
        assertEquals(expected, result);
    }

    public void assertUnit(String expected, String source) {
        assertUnit(expected, source, new JolkContext());
    } 

    @Test
    public void visit_package() {
        String source = "package com.example.MyClass;";
        String expected = "package com.example.MyClass;\n";
        assertUnit(expected, source);
    }

    @Test
    public void visit_import() {
        String source = "using com.example.MyClass;";
        String expected = "import com.example.MyClass;\n";
        assertUnit(expected, source);
    }
    
    @Test
    public void visit_import_2() {
        String source = "using com.example.MyClassA;\nusing com.example.MyClassB;";
        String expected = "import com.example.MyClassA;\nimport com.example.MyClassB;\n";
        assertUnit(expected, source);
    }

    @Test
    public void visit_import_static() {
        String source = "using meta com.example.MyClass.CONST;";
        String expected = "import static com.example.MyClass.CONST;\n";
        assertUnit(expected, source);
    }

}