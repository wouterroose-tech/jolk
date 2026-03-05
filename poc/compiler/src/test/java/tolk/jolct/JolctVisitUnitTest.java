package tolk.jolct;

import org.junit.jupiter.api.Test;

import tolk.grammar.jolkParser.UnitContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import tolk.grammar.jolkParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import tolk.grammar.jolkLexer;

public class JolctVisitUnitTest extends JolctVisitorTest {

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
    public void visit_import_decl_1() {
        String source = "import com.example.MyClass;";
        String expected = "import com.example.MyClass;\n\n";
        assertUnit(expected, source);
    }
    
    @Test
    public void visit_import_decl_2() {
        String source = "import com.example.MyClassA;\n import com.example.MyClassB;";
        String expected = "import com.example.MyClassA;\nimport com.example.MyClassB;\n\n";
        assertUnit(expected, source);
    }

   @Test
    public void visit_type_decl() {
        String source = "class Test { }";
        String expected = """
            public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {
            }
            """;
        assertUnit(expected, source);
    }

   @Test
    public void visit_type_decl_1() {
        String source = "class Test { }";
        String expected = """
            public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {
            }
            """;
        assertUnit(expected, source);
    }

   @Test
    public void visit_type_decl_2() {
        String source = "class Error extends Exception { }";
        String expected = """
            public class Error extends Exception {
            }
            """;
        assertUnit(expected, source);
    }

   @Test
    public void visit_Enum_1() {
        String source = "enum Color{ }";
        String expected = """
            public enum Color {
            ;
            }
            """;
        assertUnit(expected, source);
    }

   @Test
    public void visit_Enum_2() {
        String source = "enum Color{ RED; GREEN; BLUE; }";
        String expected = """
            public enum Color {
            RED, GREEN, BLUE;
            }
            """;
        assertUnit(expected, source);
    }

   @Test
    public void visit_record_1() {
        String source = "record Point { Int x; Int y; }";
        String expected = """
            public record Point (Int x, Int y) {
            }
            """;
        assertUnit(expected, source);
    }

   @Test
    public void visit_protocol() {
        String source = """
            protocol ChildRequirement<T, R> {
                ValidationSuite<T> add(Validation<R> validation);
            }
            """;
        String expected = """
            public interface ChildRequirement<T extends jolk.lang.Object<T>, R extends jolk.lang.Object<R>, Self extends ChildRequirement<T, R, Self>> {
            public ValidationSuite<T, ?> add(Validation<R, ?> validation);
            }
            """;
        
        JolkContext context = new JolkContext();
        context.addJolkClass("ValidationSuite");
        context.addJolkClass("Validation");
        
        assertUnit(expected, source, context);
    }

   @Test
    public void visit_protocol_2() {
        String source = """
            final class ChildRequirementBridge<T, R> implements ChildRequirement<T, R> {
            }
            """;
        String expected = """
            public final class ChildRequirementBridge<T extends jolk.lang.Object<T>, R extends jolk.lang.Object<R>> extends jolk.lang.Object<ChildRequirementBridge<T, R>> implements ChildRequirement<T, R, ChildRequirementBridge<T, R>> {
            }
            """;
        
        JolkContext context = new JolkContext();
        context.addJolkType("ChildRequirement");
        assertUnit(expected, source, context);
    }

   @Test
    public void visit_crtp_type_usage() {
        String source = """
            class Test {
                MyProtocol p;
            }
            """;
        String expected = """
            public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {
            private MyProtocol<?> p;
            private MyProtocol<?> p() {
            return p;
            }
            private Self p(MyProtocol<?> p) {
            this.p = p;
            return (Self) this;
            }
            }
            """;
        
        JolkContext context = new JolkContext();
        context.addCrtpType("MyProtocol");
        assertUnit(expected, source, context);
    }

   @Test
    public void visit_non_final_class_usage() {
        String source = """
            class Consumer {
                Provider p;
            }
            """;
        String expected = """
            public class Consumer<Self extends Consumer<Self>> extends jolk.lang.Object<Self> {
            private Provider<?> p;
            private Provider<?> p() {
            return p;
            }
            private Self p(Provider<?> p) {
            this.p = p;
            return (Self) this;
            }
            }
            """;
        
        JolkContext context = new JolkContext();
        context.addJolkClass("Provider");
        assertUnit(expected, source, context);
    }

   @Test
    public void visit_final_class_usage() {
        String source = """
            class Consumer {
                FinalProvider p;
            }
            """;
        String expected = """
            public class Consumer<Self extends Consumer<Self>> extends jolk.lang.Object<Self> {
            private FinalProvider p;
            private FinalProvider p() {
            return p;
            }
            private Self p(FinalProvider p) {
            this.p = p;
            return (Self) this;
            }
            }
            """;
        
        JolkContext context = new JolkContext();
        context.addJolkType("FinalProvider");
        assertUnit(expected, source, context);
    }

   @Test
    public void visit_record_suppress_wildcard() {
        String source = "record Container { Item item; }";
        String expected = """
            public record Container (Item item) {
            }
            """;
        
        JolkContext context = new JolkContext();
        context.addJolkClass("Item");
        assertUnit(expected, source, context);
    }

   @Test
    public void visit_method_reference() {
        String source = """
            class Test {
                Object h1 = self ##method;
                Object h2 = other ##method;
            }
            """;
        String expected = """
            public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {
            private jolk.lang.Object<?> h1 = this::method;
            private jolk.lang.Object<?> h2 = other::method;
            // ... default accessors omitted for brevity in this check, but they would be generated
            """;
        // Note: We only check the field generation part logic here implicitly via the visitor output
        // For exact string match including accessors:
        String fullExpected = """
            public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {
            private jolk.lang.Object<?> h1 = this::method;
            private jolk.lang.Object<?> h1() {
            return h1;
            }
            private Self h1(jolk.lang.Object<?> h1) {
            this.h1 = h1;
            return (Self) this;
            }
            private jolk.lang.Object<?> h2 = other::method;
            private jolk.lang.Object<?> h2() {
            return h2;
            }
            private Self h2(jolk.lang.Object<?> h2) {
            this.h2 = h2;
            return (Self) this;
            }
            }
            """;
        assertUnit(fullExpected, source);
    }

   @Test
    public void visit_closure_demonstrator() {
        String source = """
            package examples;
            import jolk.lang.Array;
            class ClosureDemonstrator {
                Array<String> runClosureParam() {
                    Array<String> strings = #["a", "b", "c"];
                    ^ strings #map [ String s -> s + s ]
                }
                Array<String> runMethodReference() {
                    Array<String> strings = #["x", "y", "z"];
                    ^ strings #map(self ##doubleValue)
                }
                String doubleValue(String s) {
                    ^ s + s
                }
            }
            """;
        String expected = """
            package examples;
            
            import jolk.lang.Array;
            
            public class ClosureDemonstrator<Self extends ClosureDemonstrator<Self>> extends jolk.lang.Object<Self> {
            public jolk.lang.Array<String> runClosureParam() {
            jolk.lang.Array<String> strings = jolk.lang.Array.of("a", "b", "c");
            return strings.map((String s) -> s + s);
            }
            public jolk.lang.Array<String> runMethodReference() {
            jolk.lang.Array<String> strings = jolk.lang.Array.of("x", "y", "z");
            return strings.map(this::doubleValue);
            }
            public String doubleValue(String s) {
            return s + s;
            }
            }
            """;
        assertUnit(expected, source);
    }

}