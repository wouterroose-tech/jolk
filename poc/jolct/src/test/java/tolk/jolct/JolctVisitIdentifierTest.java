package tolk.jolct;

import org.junit.jupiter.api.Test;

public class JolctVisitIdentifierTest extends JolctVisitorTest {

    @Test
    public void testMetaIdentifier() {
        String source = "class MyClass { }";
        String expected = "public class MyClass<Self extends MyClass<Self>> extends jolk.lang.Object<Self> {\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testMetaIdentifierWithUnderscore() {
        String source = "class MY_CLASS { }";
        String expected = "public class MY_CLASS<Self extends MY_CLASS<Self>> extends jolk.lang.Object<Self> {\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testIdentifierID() {
        String source = "class A { Int x; }";
        String expected = "public class A<Self extends A<Self>> extends jolk.lang.Object<Self> {\nprivate Int x;\nprivate Int x() {\nreturn x;\n}\nprivate Self x(Int x) {\nthis.x = x;\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testIdentifierIDWithUnderscore() {
        String source = "class A { Int my_var; }";
        String expected = "public class A<Self extends A<Self>> extends jolk.lang.Object<Self> {\nprivate Int my_var;\nprivate Int my_var() {\nreturn my_var;\n}\nprivate Self my_var(Int my_var) {\nthis.my_var = my_var;\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testMetaConstant_PublicStaticFinal() {
        String source = "class Config { meta constant Interrupt FORM_INTERRUPT = Interrupt #new; }";
        String expected = "public class Config<Self extends Config<Self>> extends jolk.lang.Object<Self> {\nprivate static final Interrupt FORM_INTERRUPT = new Interrupt();\nprivate static Interrupt FORM_INTERRUPT() {\nreturn FORM_INTERRUPT;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }
}