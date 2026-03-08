package tolk.jolct;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JolctVisitFieldTest extends JolctVisitorTest {

    private void assertTranspilation(String expectedBody, String sourceBody) {
        String source = "class Test { " + sourceBody + " }";
        // Note: JolctVisitor adds 4 spaces indent for members
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\n" + expectedBody + "}\n";
        assertEquals(expected, transpile(source));
    }

    @Test
    public void testBaseField() {
        assertTranspilation("private Type supplier;\n" +
                "private Type supplier() {\n" +
                "return supplier;\n" +
                "}\n" +
                "private Self supplier(Type supplier) {\n" +
                "this.supplier = supplier;\n" +
                "return (Self) this;\n" +
                "}\n", "Type supplier;");
    }

    @Test
    public void testBaseFieldWithInit() {
        assertTranspilation("private Type supplier = new Type();\n" +
                "private Type supplier() {\n" +
                "return supplier;\n" +
                "}\n" +
                "private Self supplier(Type supplier) {\n" +
                "this.supplier = supplier;\n" +
                "return (Self) this;\n" +
                "}\n", "Type supplier = Type #new;");
    }

    @Test
    public void testGenericField() {
        assertTranspilation("private Type<T> supplier;\n" +
                "private Type<T> supplier() {\n" +
                "return supplier;\n" +
                "}\n" +
                "private Self supplier(Type<T> supplier) {\n" +
                "this.supplier = supplier;\n" +
                "return (Self) this;\n" +
                "}\n", "Type<T> supplier;");
    }

    @Test
    public void testGenericFieldWithInit() {
        assertTranspilation("private Type<T> supplier = new Type();\n" +
                "private Type<T> supplier() {\n" +
                "return supplier;\n" +
                "}\n" +
                "private Self supplier(Type<T> supplier) {\n" +
                "this.supplier = supplier;\n" +
                "return (Self) this;\n" +
                "}\n", "Type<T> supplier = Type #new;");
    }

    @Test
    public void testVisibilityNone() {
        assertTranspilation("private Type field;\n" +
                "private Type field() {\n" +
                "return field;\n" +
                "}\n" +
                "private Self field(Type field) {\n" +
                "this.field = field;\n" +
                "return (Self) this;\n" +
                "}\n", "Type field;");
    }

    @Test
    public void testVisibilityPackage() {
        assertTranspilation("Type field;\n" +
                "Type field() {\n" +
                "return field;\n" +
                "}\n" +
                "Self field(Type field) {\n" +
                "this.field = field;\n" +
                "return (Self) this;\n" +
                "}\n", "package Type field;");
    }

    @Test
    public void testVisibilityProtected() {
        assertTranspilation("protected Type field;\n" +
                "protected Type field() {\n" +
                "return field;\n" +
                "}\n" +
                "protected Self field(Type field) {\n" +
                "this.field = field;\n" +
                "return (Self) this;\n" +
                "}\n", "protected Type field;");
    }
    
    @Test
    public void testVisibilityPublic() {
        assertTranspilation("public Type field;\n" +
                "public Type field() {\n" +
                "return field;\n" +
                "}\n" +
                "public Self field(Type field) {\n" +
                "this.field = field;\n" +
                "return (Self) this;\n" +
                "}\n", "public Type field;");
    }

    @Test
    public void testConstant() {
        // constant -> final. Default visibility private.
        assertTranspilation("private final Type PI = 3.14;\n" +
                "private Type PI() {\n" +
                "return PI;\n" +
                "}\n", "constant Type PI = 3.14;");
    }

    @Test
    public void testMeta() {
        // meta -> static. Default visibility private.
        assertTranspilation("private static Type instance;\n" +
                "private static Type instance() {\n" +
                "return instance;\n" +
                "}\n" +
                "private static void instance(Type instance) {\n" +
                "Test.instance = instance;\n" +
                "}\n", "meta Type instance;");
    }

    @Test
    public void testMetaPublicConstant() {
        assertTranspilation("public static final Type VERSION = \"1.0\";\n" +
                "public static Type VERSION() {\n" +
                "return VERSION;\n" +
                "}\n", "public meta constant Type VERSION = \"1.0\";");
    }

    @Test
    public void testJavaGenericFieldWithInit() {
        assertTranspilation("private java.util.ArrayList<String> list = new java.util.ArrayList();\n" +
                "private java.util.ArrayList<String> list() {\n" +
                "return list;\n" +
                "}\n" +
                "private Self list(java.util.ArrayList<String> list) {\n" +
                "this.list = list;\n" +
                "return (Self) this;\n" +
                "}\n", "java.util.ArrayList<String> list = java.util.ArrayList #new;");
    }

    @Test
    public void testImportedJolkClassField() {
        String source = "class Test { ImportedType field; }";
        String expected = "public class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\nprivate ImportedType<?> field;\nprivate ImportedType<?> field() {\nreturn field;\n}\nprivate Self field(ImportedType<?> field) {\nthis.field = field;\nreturn (Self) this;\n}\n}\n";
        
        JolkContext context = new JolkContext();
        context.addJolkClass("ImportedType");
        assertEquals(expected, transpile(source, context));
    }

    @Test
    public void testSamePackageJolkClassField() {
        String source = "package com.test; class Test { SiblingType field; }";
        String expected = "package com.test;\npublic class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\nprivate SiblingType<?> field;\nprivate SiblingType<?> field() {\nreturn field;\n}\nprivate Self field(SiblingType<?> field) {\nthis.field = field;\nreturn (Self) this;\n}\n}\n";
        
        JolkContext context = new JolkContext();
        context.addJolkClass("com.test.SiblingType");
        assertEquals(expected, transpile(source, context));
    }

    @Test
    public void testExplicitlyImportedJolkClassField() {
        String source = "using com.other.OtherType; class Test { OtherType field; }";
        String expected = "import com.other.OtherType;\npublic class Test<Self extends Test<Self>> extends jolk.lang.Object<Self> {\nprivate OtherType<?> field;\nprivate OtherType<?> field() {\nreturn field;\n}\nprivate Self field(OtherType<?> field) {\nthis.field = field;\nreturn (Self) this;\n}\n}\n";
        
        JolkContext context = new JolkContext();
        context.addJolkClass("com.other.OtherType");
        assertEquals(expected, transpile(source, context));
    }

    @Test
    public void testMetaPublicField() {
        assertTranspilation("public static Int timeout;\n" +
                "public static Int timeout() {\n" +
                "return timeout;\n" +
                "}\n" +
                "public static void timeout(Int timeout) {\n" +
                "Test.timeout = timeout;\n" +
                "}\n", "public meta Int timeout;");
    }

    @Test
    public void testNoDuplicateGetter() {
        assertTranspilation("public Int x;\n" +
                "public Self x(Int x) {\n" +
                "this.x = x;\n" +
                "return (Self) this;\n" +
                "}\n" +
                "public Int x() {\n" +
                "return x;\n" +
                "}\n", 
                "public Int x; public Int x() { ^ x }");
    }

    @Test
    public void testNoDuplicateSetter() {
        assertTranspilation("public Int x;\n" +
                "public Int x() {\n" +
                "return x;\n" +
                "}\n" +
                "public Self x(Int v) {\n" +
                "x = v;\n" +
                "return (Self) this;\n" +
                "}\n", 
                "public Int x; public Self x(Int v) { x = v; ^ self }");
    }

    @Test
    public void testGenericConstant() {
        assertTranspilation("private final List<String> NAMES = new ArrayList();\n" +
                "private List<String> NAMES() {\n" +
                "return NAMES;\n" +
                "}\n", "constant List<String> NAMES = ArrayList #new;");
    }
}
