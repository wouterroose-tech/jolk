package tolk.jolct;

import org.junit.jupiter.api.Test;

public class JolctVisitMetaTest extends JolctVisitorTest {

    @Test
    public void testObjectFieldTranspilation() {
        // 1/ fields or params with type Object become -> jolk.lang.Object
        String source = "class Container { Object item; }";
        // Expect explicit jolk.lang.Object to avoid ambiguity with java.lang.Object
        // CRTP applied to class definition
        String expected = "public class Container<Self extends Container<Self>> extends jolk.lang.Object<Self> {\nprivate jolk.lang.Object<?> item;\nprivate jolk.lang.Object<?> item() {\nreturn item;\n}\nprivate Self item(jolk.lang.Object<?> item) {\nthis.item = item;\nreturn (Self) this;\n}\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testImplicitExtension() {
        // 2/ every class that doesn't extend must extend from jolk.lang.Object
        // CRTP applied: class MyClass<Self extends MyClass<Self>> extends jolk.lang.Object<Self>
        String source = "class MyClass { }";
        String expected = "public class MyClass<Self extends MyClass<Self>> extends jolk.lang.Object<Self> {\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testExplicitExtension() {
        // Verify that explicit extension is respected and no double extension is added
        // CRTP applied: class MySubClass<Self extends MySubClass<Self>> extends MyClass<Self>
        String source = "class MySubClass extends MyClass { }";
        String expected = "public class MySubClass<Self extends MySubClass<Self>> extends MyClass<Self> {\n}\n";
        JolkContext context = new JolkContext();
        context.addJolkClass("MyClass");
        String result = transpile(source, context);
        org.junit.jupiter.api.Assertions.assertEquals(expected.replace("\r\n", "\n").trim(), result.replace("\r\n", "\n").trim());
    }

    @Test
    public void testMetaClassExtension() {
        // 3/ MetaClass must extend jolk.lang.Object to participate in the unified messaging protocol.
        // It is a first-class citizen in the Jolk type system.
        String source = "class MetaClass { }";
        String expected = "public class MetaClass<Self extends MetaClass<Self>> extends jolk.lang.Object<Self> {\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testFinalClassExtension() {
        // Final class: Self is the type itself
        // final class MyFinal extends jolk.lang.Object<MyFinal>
        String source = "final class MyFinal { }";
        String expected = "public final class MyFinal extends jolk.lang.Object<MyFinal> {\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testGenericClassExtension() {
        // Generic class: CRTP with generics
        // class Box<T, Self extends Box<T, Self>> extends jolk.lang.Object<Self>
        String source = "class Box<T> { }";
        String expected = "public class Box<T extends jolk.lang.Object<T>, Self extends Box<T, Self>> extends jolk.lang.Object<Self> {\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testJavaExtension() {
        // Non-terminal Jolk classes extending from java classes (like Exception) must apply the CRTP pattern
        // but the super class (Exception) does not take the Self generic.
        String source = "class MyException extends Exception { }";
        String expected = "public class MyException extends Exception {\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testJavaRuntimeExceptionExtension() {
        String source = "class MyRuntime extends RuntimeException { }";
        String expected = "public class MyRuntime extends RuntimeException {\n}\n";
        this.assertFullTranspilation(expected, source);
    }

    @Test
    public void testJavaExtensionFinal() {
        String source = "final class Interrupt extends Exception { }";
        String expected = "public final class Interrupt extends Exception {\n}\n";
        this.assertFullTranspilation(expected, source);
    }
}
