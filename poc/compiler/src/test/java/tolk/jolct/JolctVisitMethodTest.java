package tolk.jolct;

import org.junit.jupiter.api.Test;

import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkParser.UnitContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class JolctVisitMethodTest extends JolctVisitorTest {

    void assertMethod(String expected, String source) {
        JolkContext context = new JolkContext();
        JolctVisitor visitor = new JolctVisitor(context);
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        jolkParser parser = new jolkParser(tokens);
        UnitContext unitContext = parser.unit();
        String result = visitor.visitUnit(unitContext);
        assertEquals(expected, result);
    }

    @Test
    void testMethod() {
        String source = """
            class Test[T] {
                public Self do(T subject) {
                    !(self #isValid(subject)) ? [^ self];
                    ^ self
                }
            }
            """;
        String expected = """
            public class Test<T extends jolk.lang.Object<T>, Self extends Test<T, Self>> extends jolk.lang.Object<Self> {
            public Self do(T subject) {
            if (!(this.isValid(subject))) {
            return (Self) this;
            }
            return (Self) this;
            }
            }
            """;
        assertMethod(expected, source);
    }

    @Test
    void testImplicitReturn() {
        String source = """
            class Test {
                Self do() {
                    node #do()
                }
            }
            """;
        String expected = """
            public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {
            public Self do() {
            node.do();
            return (Self) this;
            }
            }
            """;
        assertMethod(expected, source);
    }

    @Test
    public void testEmptyMethodDefaultReturn() {
        // add default return null
        String source = "class Test { Object method() {} }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic jolk.lang.Object<?> method() {\nreturn null;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testPublicByDefault() {
        // if no modifier -> public
        String source = "class Test { Object method() {} }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic jolk.lang.Object<?> method() {\nreturn null;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testPackageModifier() {
        // if package -> remove (Java default is package-private)
        String source = "class Test { package Object method() {} }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\njolk.lang.Object<?> method() {\nreturn null;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testSelfReturnNonFinal() {
        // Self return type of non-final it's Self
        String source = "class Test { Self method() {} }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self method() {\nreturn (Self) this;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testSelfReturnFinal() {
        // Self return type of final class is the Type itself
        // final class Self becomes the class
        String source = "final class Test { Self method() {} }";
        String expected = "public final class Test extends jolk.lang.Object<Test> {\npublic Test method() {\nreturn this;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testMethodGenerics() {
        // convert the type param brackits [] to <>
        String source = "class Test { [T] Object method() {} }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic <T extends jolk.lang.Object<T>> jolk.lang.Object<?> method() {\nreturn null;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testMetaMethod() {
        // meta method -> static method
        String source = "class Test { meta Object method() {} }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic static jolk.lang.Object<?> method() {\nreturn null;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testAbstractMethod() {
        // abstract method -> semicolon, no body
        String source = "abstract class Test { abstract Object method(); }";
        String expected = "public abstract class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic abstract jolk.lang.Object<?> method();\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testMethodWithAnnotatedParams() {
        String source = "class ChildValidation[T] { Self accept(@NotNull T subject, @NotNull Object context) {} }";
        String expected = "public class ChildValidation<T extends jolk.lang.Object<T>, Self extends ChildValidation<T, Self>> extends jolk.lang.Object<Self> {\npublic Self accept(@NotNull T subject, @NotNull jolk.lang.Object<?> context) {\nreturn (Self) this;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testEquals() {
        // ~~ -> equals
        // jolk.lang.Object or java.lang.Object
        String source = "class Test { Boolean ~~(Object other) { ^ false} }";
        String expected = """
            public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {
            public boolean equals(Object other) {
            return false;
            }
            }
            """;
        assertMethod(expected, source);
    }

    @Test
    public void testConstructorGeneration() {
        // meta new -> Constructor
        String source = "class Test { meta Test new() {} }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Test() {\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testConstructorWithSuperAndChaining() {
        // ^ super #new #init -> super(); this.init();
        String source = "class Test { meta Test new() { ^ super #new #init } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Test() {\nsuper(); this.init();\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testGenericFinalClassMethod() {
        String source = "final class ChildValidation[T, R] extends Node[T] { Self accept(T subject) {} }";
        String expected = "public final class ChildValidation<T extends jolk.lang.Object<T>, R extends jolk.lang.Object<R>> extends Node<T, ChildValidation<T, R>> {\npublic ChildValidation<T, R> accept(T subject) {\nreturn this;\n}\n}\n";
        JolkContext context = new JolkContext();
        context.addJolkType("Node");
        assertFullTranspilation(expected, source, context);
    }

    @Test
    public void testExecutionContextCRTP() {
        String source = "class ExecutionContext { ExecutionContext add(Object subject, Issue issue) {} }";
        String expected = "public class ExecutionContext<Self extends ExecutionContext<Self>> extends jolk.lang.Object<Self> {\npublic ExecutionContext add(jolk.lang.Object<?> subject, Issue issue) {\nreturn null;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testImplicitSelfReturnWithBody() {
        String source = "class Test { Self method() { 1 } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self method() {\n1;\nreturn (Self) this;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testExplicitSelfReturn() {
        String source = "class Test { Self method() { ^ null } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self method() {\nreturn null;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testExplicitSelfReturnThis() {
        String source = "class Test { Self method() { ^ self } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self method() {\nreturn (Self) this;\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testReturnIfThen() {
        String source = "class Test { Self m(Bool b) { ^b ? self : null } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self m(Bool b) {\nif (b) {\nreturn (Self) this;\n} else {\nreturn null;\n}\n}\n}\n";
        assertMethod(expected, source);
    }

    @Test
    public void testReturnIfThenBlock() {
        String source = "class Test { Self m(Bool b) { b ? [^self] : [^null] } }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\npublic Self m(Bool b) {\nif (b) {\nreturn (Self) this;\n} else {\nreturn null;\n}\n}\n}\n";
        assertMethod(expected, source);
    }
}