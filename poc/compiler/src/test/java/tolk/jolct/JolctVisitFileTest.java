package tolk.jolct;

import org.junit.jupiter.api.Test;

public class JolctVisitFileTest extends JolctVisitorTest {

    @Test
    public void testVisitUnitContext() {
        String source = """
                package com.example;
                using java.util.List;
                class MyClass { }
                """;

        // Expected behavior:
        // 1. Package decl adds double newline
        // 2. No default import jolk.lang.*;
        // 3. Preserves explicit import
        // 4. Adds newline before type
        // 5. Generates public class
        String expected = """
                package com.example;
                import java.util.List;
                public class MyClass<Self extends MyClass<Self>> extends jolk.lang.Object<Self> {
                }
                """;

        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testVisitUnitContext_JolkLang() {
        String source = "package jolk.lang; class MyClass { }";
        String expected = """
                package jolk.lang;
                public class MyClass<Self extends MyClass<Self>> extends jolk.lang.Object<Self> {
                }
                """;

        this.assertFullTranspilation(expected, source);
    }

    @Test
    void testClassTranspilation() {
        String source = "package com.test; class MyClass {}";
        String expected = """
                package com.test;
                public class MyClass<Self extends MyClass<Self>> extends jolk.lang.Object<Self> {
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testVisibilityPackage() {
        String source = "package class MyClass {}";
        String expected = """
                class MyClass<Self extends MyClass<Self>> extends jolk.lang.Object<Self> {
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testVisibilityDefault() {
        String source = "class MyClass {}";
        String expected = """
                public class MyClass<Self extends MyClass<Self>> extends jolk.lang.Object<Self> {
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testVisibilityExplicitPublic() {
        String source = "public class MyClass {}";
        String expected = """
                public class MyClass<Self extends MyClass<Self>> extends jolk.lang.Object<Self> {
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testVisibilityExplicitPrivate() {
        String source = "private class MyClass {}";
        String expected = """
                private class MyClass<Self extends MyClass<Self>> extends jolk.lang.Object<Self> {
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testVisibilityExplicitProtected() {
        String source = "protected class MyClass {}";
        String expected = """
                protected class MyClass<Self extends MyClass<Self>> extends jolk.lang.Object<Self> {
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testClassWithGeneratedAccessors() {
        // Fields are private by default (accessors generated)
        // Public fields should generate public accessors
        String source = "class Point { Int x; public Int y; }";
        String expected = """
                public class Point<Self extends Point<Self>> extends jolk.lang.Object<Self> {
                private Int x;
                private Int x() {
                return x;
                }
                private Self x(Int x) {
                this.x = x;
                return (Self) this;
                }
                public Int y;
                public Int y() {
                return y;
                }
                public Self y(Int y) {
                this.y = y;
                return (Self) this;
                }
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testFinalClassWithPublicField() {
        String source = "final class Person { public Long ssn; }";
        String expected = """
                public final class Person extends jolk.lang.Object<Person> {
                public Long ssn;
                public Long ssn() {
                return ssn;
                }
                public Person ssn(Long ssn) {
                this.ssn = ssn;
                return (Person) this;
                }
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testClassWithNew() {
        String source = "class Person { meta Person new() { ^ super #new } }";
        String expected = """
                public class Person<Self extends Person<Self>> extends jolk.lang.Object<Self> {
                public Person() {
                super();
                }
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testClassWithMethod() {
        String source = "final class Person { String do() { constant String s = person #toString; ^ s} }";
        String expected = """
                public final class Person extends jolk.lang.Object<Person> {
                public String do() {
                final String s = person.toString();
                return s;
                }
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testClassWithMethod_2() {
        String source = "final class Person { String do() { s = person #toString; ^ s} }";
        String expected = """
                public final class Person extends jolk.lang.Object<Person> {
                public String do() {
                s = person.toString();
                return s;
                }
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testClassWithConstant() {
        String source = "final class Person { constant String s = SYMBOL;}";
        String expected = """
                public final class Person extends jolk.lang.Object<Person> {
                private final String s = SYMBOL;
                private String s() {
                return s;
                }
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testClassWithMetaConstant() {
        String source = "final class Person { meta constant String s = SYMBOL;}";
        String expected = """
                public final class Person extends jolk.lang.Object<Person> {
                private static final String s = SYMBOL;
                private static String s() {
                return s;
                }
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testClassWithCanonicalNew() {
        String source = "class Person { public Long ssn; meta Person new() { ^ super #new } }";
        String expected = """
                public class Person<Self extends Person<Self>> extends jolk.lang.Object<Self> {
                public Long ssn;
                public Long ssn() {
                return ssn;
                }
                public Self ssn(Long ssn) {
                this.ssn = ssn;
                return (Self) this;
                }
                public Person() {
                super();
                }
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    void testFinalClassWithCanonicalNew() {
        String source = "final class Person { public Long ssn; meta Person new() { ^ super #new } }";
        String expected = """
                public final class Person extends jolk.lang.Object<Person> {
                public Long ssn;
                public Long ssn() {
                return ssn;
                }
                public Person ssn(Long ssn) {
                this.ssn = ssn;
                return (Person) this;
                }
                public Person() {
                super();
                }
                }
                """;
        assertFullTranspilation(expected, source);
    }
    

    @Test
    void testClassExtendsRuntimeException() {
        String source = """
                final class Interrupt extends RuntimeException {
                public meta constant Interrupt HALT = Interrupt #new;
                meta Interrupt HALT() { ^ HALT }
                }
                """;;
        String expected = """
            public final class Interrupt extends RuntimeException {
            public static final Interrupt HALT = new Interrupt();
            public static Interrupt HALT() {
            return HALT;
            }
            }
                """;
        assertFullTranspilation(expected, source);
    }
}
