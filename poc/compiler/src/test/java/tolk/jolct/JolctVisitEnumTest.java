package tolk.jolct;

import org.junit.jupiter.api.Test;

public class JolctVisitEnumTest extends JolctVisitorTest {

    @Test
    public void testVisitEnum() {
        String source = "enum Color { RED; GREEN; BLUE; }";
        String expected = "public enum Color {\nRED, GREEN, BLUE;\n}\n";

        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testVisitEnum_Package() {
        String source = "package enum Status { ON; OFF; }";
        String expected = "enum Status {\nON, OFF;\n}\n";

        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testVisitEnum_Private() {
        String source = "private enum Status { ON; OFF; }";
        String expected = "private enum Status {\nON, OFF;\n}\n";

        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testVisitEnum_Level() {
        String source = "package demo.validation.engine; enum Level { ERROR; WARNING; INFO; }";
        String expected = "package demo.validation.engine;\n\npublic enum Level {\nERROR, WARNING, INFO;\n}\n";

        this.assertFullTranspilation(expected, source);
    }

    @Test
    void testEnumWithPackage() {
        String source = "package com.test; enum Color { RED; GREEN; BLUE; }";
        String expected = """
                package com.test;

                public enum Color {
                RED, GREEN, BLUE;
                }
                """;
        assertFullTranspilation(expected, source);
    }

    @Test
    public void testEnumConstantAccess() {
        String source = "class Test { Object l = Level #ERROR; }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\nprivate jolk.lang.Object<?> l = Level.ERROR;\nprivate jolk.lang.Object<?> l() {\nreturn l;\n}\nprivate Self l(jolk.lang.Object<?> l) {\nthis.l = l;\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }
}
