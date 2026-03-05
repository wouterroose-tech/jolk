package tolk.jolct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkParser.ExpressionContext;

public class JolctVisitExpressionTest extends JolctVisitorTest {
    

    void assertExpression(String expected, String source) {
        JolkContext context = new JolkContext();
        JolctVisitor visitor = new JolctVisitor(context);
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        ExpressionContext expressionContext = parser.expression();
        String result = visitor.visitExpression(expressionContext);
        assertEquals(expected, result);
    }

    @Test
    void testNullCoalescing() {
        String source = "s ?? \"Default\"";
        String expected = "(s != null ? s : \"Default\")";
        assertExpression(expected, source);
    }

    @Test
    void testMethodReference() {
        String source = "messages #forEach(StringUtils ##capitalize)";
        String expected = "messages.forEach(StringUtils::capitalize)";
        assertExpression(expected, source);
    }

    @Test
    void testMethodReference_1() {
        String source = "messages #forEach(self ##capitalize)";
        String expected = "messages.forEach(this::capitalize)";
        assertExpression(expected, source);
    }

    @Test
    void testMethodReference_2() {
        String source = "messages #forEach(x ##capitalize)";
        String expected = "messages.forEach(x::capitalize)";
        assertExpression(expected, source);
    }

    @Test
    void testClosureParam() {
        String source = "colors #forEach [color -> result #append(color)]";
        String expected = "colors.forEach(color -> result.append(color))";
        assertExpression(expected, source);
    }

    @Test
    void testClosureParam_2() {
        String source = "colors #forEach [Color color -> result #append(color)]";
        String expected = "colors.forEach((Color color) -> result.append(color))";
        assertExpression(expected, source);
    }

    @Test
    void testIdentityExpression() {
        String source = "x ~~ y";
        String expected = "java.util.Objects.equals(x, y)";
        assertExpression(expected, source);
    }

    @Test
    void testNonIdentityExpression() {
        String source = "x !~ y";
        String expected = "(!java.util.Objects.equals(x, y))";
        assertExpression(expected, source);
    }

    @Test
    public void testNewInstance() {
        String source = "class Test { Object o = Type #new; }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\nprivate jolk.lang.Object<?> o = new Type();\nprivate jolk.lang.Object<?> o() {\nreturn o;\n}\nprivate Self o(jolk.lang.Object<?> o) {\nthis.o = o;\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testNewInstanceWithArgs() {
        String source = "class Test { Object o = Type #new(1, \"a\"); }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\nprivate jolk.lang.Object<?> o = new Type(1, \"a\");\nprivate jolk.lang.Object<?> o() {\nreturn o;\n}\nprivate Self o(jolk.lang.Object<?> o) {\nthis.o = o;\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }
    
    @Test
    public void testMethodCall() {
        String source = "class Test { Object o = obj #method; }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\nprivate jolk.lang.Object<?> o = obj.method();\nprivate jolk.lang.Object<?> o() {\nreturn o;\n}\nprivate Self o(jolk.lang.Object<?> o) {\nthis.o = o;\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);

    }

    @Test
    public void testMethodCallWithArgs() {
        String source = "class Test { Object o = obj #method(1); }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\nprivate jolk.lang.Object<?> o = obj.method(1);\nprivate jolk.lang.Object<?> o() {\nreturn o;\n}\nprivate Self o(jolk.lang.Object<?> o) {\nthis.o = o;\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testSelfMessage() {
        String source = "class Test { Object o = self #method; }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\nprivate jolk.lang.Object<?> o = this.method();\nprivate jolk.lang.Object<?> o() {\nreturn o;\n}\nprivate Self o(jolk.lang.Object<?> o) {\nthis.o = o;\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testNewInstanceWithGenerics() {
        String source = "class Test { Object o = List[String] #new; }";
        String expected = """
            public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {
            private jolk.lang.Object<?> o = new List<String>();
            private jolk.lang.Object<?> o() {
            return o;
            }
            private Self o(jolk.lang.Object<?> o) {
            this.o = o;
            return (Self) this;
            }
            }""".trim();
        this.assertFullTranspilation(expected, source);
    }
}
