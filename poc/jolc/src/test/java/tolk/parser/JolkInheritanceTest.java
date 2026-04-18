package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkInheritanceTest  extends JolcTestBase {

    @Test
    void testInheritAccessors() {
        String classC = "class ClassC extends ClassB { }";
        String classA = "class ClassA { public Long x; }";
        String classB = "class ClassB extends ClassA { }";
        Value instanceA = eval(classA).invokeMember("new");
        assertEquals(0, instanceA.invokeMember("x").asLong());
        Value instanceB = eval(classB).invokeMember("new");
        assertEquals(0, instanceB.invokeMember("x").asLong());
        assertEquals(instanceB, instanceB.invokeMember("x", 42));
        assertEquals(42, instanceB.invokeMember("x").asLong());
        // This tests that the inheritance chain is properly established
        // and that ClassC can access the field 'x' defined in ClassA through ClassB.
        Value instanceC= eval(classC).invokeMember("new");
        assertEquals(0, instanceC.invokeMember("x").asLong());
        assertEquals(instanceC, instanceC.invokeMember("x", 42));
        assertEquals(42, instanceC.invokeMember("x").asLong());
    }

    @Test
    void testInheritMethods() {
        String classA = "class ClassA { Long x() { ^ 42 } }";
        String classB = "class ClassB extends ClassA { }";
        Value instanceA = eval(classA).invokeMember("new");
        assertEquals(42, instanceA.invokeMember("x").asLong());
        Value instanceB = eval(classB).invokeMember("new");
        assertEquals(42, instanceB.invokeMember("x").asLong());
    }

    @Test
    void testOverrideMethods() {
        String classA = "class ClassA { Long x() { ^ 0 } }";
        String classB = "class ClassB extends ClassA { Long x() { ^ 42 } }";
        String classC = "class ClassC extends ClassA { Long x() { ^ super #x } }";
        Value instanceA = eval(classA).invokeMember("new");
        assertEquals(0, instanceA.invokeMember("x").asLong());
        Value instanceB = eval(classB).invokeMember("new");
        assertEquals(42, instanceB.invokeMember("x").asLong());
        Value instanceC = eval(classC).invokeMember("new");
        assertEquals(0, instanceC.invokeMember("x").asLong());
    }

    @Test
    void testOverrideNew() {
        String classA = "class ClassA { Long x = 42; }";
        String classB = "class ClassB extends ClassA { meta new() { ^super #new } }";
        eval(classA);
        Value instanceB = eval(classB).invokeMember("new");
        assertTrue(instanceB.invokeMember("class").getMetaSimpleName().contains("ClassB"));
        assertEquals(42, instanceB.invokeMember("x").asLong());
    }

    @Test
    void testOverrideNew_2() {
        String classA = "class ClassA { Long x; }";
        String classB = "class ClassB extends ClassA { meta new() { ^super #new #x(42) } }";
        eval(classA);
        Value instanceB = eval(classB).invokeMember("new");
        assertEquals(42, instanceB.invokeMember("x").asLong());
    }

}
