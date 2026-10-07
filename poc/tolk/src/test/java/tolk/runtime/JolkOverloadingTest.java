package tolk.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkOverloadingTest extends JolcTestBase {

    @Test
    void testOverloading() {
        String source = """
            class Test {
                Long x() { ^ 1 }
                Long x(Long x) { ^ x }
                Long test() { ^ #x + #x(1) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertEquals(1, instance.invokeMember("x").asLong());
        instance = meta.invokeMember("new");
        assertEquals(42, instance.invokeMember("x", 42).asLong());
        instance = meta.invokeMember("new");
        assertEquals(2, instance.invokeMember("test").asLong());
    }

    @Test
    void testOverloading_2() {
        String source = """
            class Test {
                Long x() { ^ 1 }
                Long x(Long x) { ^ x }
                Long x(Long x, Long y) { ^ x + y }
                Long test() { ^ #x + #x(1) + #x(20, 20) }
            }""";
        eval(source);
        Value instance = eval(source).invokeMember("new");
        assertEquals(1, instance.invokeMember("x").asLong());
        instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("x", 42).asLong());
        instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("x", 2, 40).asLong());
        instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("test").asLong());
    }

    @Test
    void testOverloadingParameters() {
        String source = """
            class Test {
                Long x;
                String s;
                set() { #x(1) }
                set(String s) { #s(s) }
                set(Boolean b) { #x(b ? 3 : 0) }
            }""";
        eval(source);
        Value instance = eval(source).invokeMember("new");
        instance.invokeMember("set");
        assertEquals(1, instance.invokeMember("x").asLong());
        instance.invokeMember("set", true);
        assertEquals(3, instance.invokeMember("x").asLong());
        instance.invokeMember("set", "foo");
        assertEquals("foo", instance.invokeMember("s").toString());
    }

    @Test
    void testOverloadingWithSelfCall() {
        String source = """
            class DomainClass {
                Long x = 42;
            }""";
        eval(source);
        source = """
            class Test  {
                Long x(DomainClass d) { ^ self #x(d #x) }
                Long x(Long x) { ^ x }
                Long x() { ^ self #x(DomainClass #new) }
                Long test() { ^ self #x }
            }""";
        eval(source);
        Value instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("x").asLong());
        instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("x", 42).asLong());
        instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("test").asLong());
    }

    @Test
    void testOverloadingWithImplcitSelfCall() {
        String source = """
            class DomainClass {
                Long x = 42;
            }""";
        eval(source);
        source = """
            class Test  {
                Long x(Long x) { ^ x }
                Long x(DomainClass d) { ^ #x(d #x) }
                Long x() { ^ #x(DomainClass #new) }
                Long test() { ^ #x }
            }""";
        eval(source);
        Value instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("x").asLong());
        instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("x", 42).asLong());
        instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("test").asLong());
    }

    @Test
    void testOverloadingIntrinsicNew() {
        String source = """
            class Test {
                Long x;
                meta new() { ^ super #new #x(42) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertEquals(42, instance.invokeMember("x").asLong());
    }

    @Test
    void testOverloadingNew() {
        String source = """
            class Test {
                Long x;
                meta new(Long x) { ^ super #new #x(x) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new", 42);
        assertEquals(42, instance.invokeMember("x").asLong());
    }

    @Test
    @Disabled("TODO: fix #new overloading")
    void testOverloadingNew_2() {
        String source = """
            class Test {
                Long x;
                meta new(Long x) { ^ self #new #x(x) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new", 42);
        assertEquals(42L, instance.invokeMember("x").asLong());
    }

    @Test
    @Disabled("TODO: fix #new overloading")
    void testOverloadingNew_3() {
        String source = """
            class Test {
                Long x;
                meta new(Long x) { ^ #new #x(x) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new", 42);
        assertEquals(42L, instance.invokeMember("x").asLong());
    }

    @Test
    void testOverloadingNew_4() {
        String source = """
            class Test {
                Long x;
                meta new() { ^ self #new(1) }
                meta new(Long x) { ^ super #new #x(x) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertEquals(1L, instance.invokeMember("x").asLong());
        instance = meta.invokeMember("new", 42);
        assertEquals(42, instance.invokeMember("x").asLong());
    }

    @Test
    void testOverloadingNew_5() {
        String source = """
            class Test {
                Long x;
                meta new() { ^ #new(1) }
                meta new(Long x) { ^ super #new #x(x) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertEquals(1, instance.invokeMember("x").asLong());
        instance = meta.invokeMember("new", 42);
        assertEquals(42, instance.invokeMember("x").asLong());
    }

    @Test
    void testOverloadingNew_6() {
        String source = """
            class Test {
                Long x;
                meta new() { ^ super #new }
                meta new(Long x) { ^ #new #x(x) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertEquals(0, instance.invokeMember("x").asLong());
        instance = meta.invokeMember("new", 42);
        assertEquals(42, instance.invokeMember("x").asLong());
    }

    @Test
    void testOverloadingNew_7() {
        String source = """
            class Test {
                Long x;
                meta new(Long x) { ^ super #new #x(x) }
                meta new(Long x, Long y) { ^ super #new #x(x + y) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new", 42);
        assertEquals(42, instance.invokeMember("x").asLong());
        instance = meta.invokeMember("new", 40, 2);
        assertEquals(42, instance.invokeMember("x").asLong());
    }

    @Test
    @Disabled("TODO: fix #new overloading")
    void testOverloadingNew_8() {
        String source = """
            class Test {
                Long x;
                meta new(Long x) { ^ self #new #x(x) }
                meta new(Long x, Long y) { ^ self #new #x(x + y) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new", 42);
        assertEquals(42, instance.invokeMember("x").asLong());
        instance = meta.invokeMember("new", 40, 2);
        assertEquals(42, instance.invokeMember("x").asLong());
    }

    @Test
    @Disabled("TODO: fix #new overloading")
    void testOverloadingNew_9() {
        String source = """
            class Test {
                Long x;
                meta new(Long x) { ^ #new #x(x) }
                meta new(Long x, Long y) { ^ #new #x(x + y) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new", 42);
        assertEquals(42, instance.invokeMember("x").asLong());
        instance = meta.invokeMember("new", 40, 2);
        assertEquals(42, instance.invokeMember("x").asLong());
    }

    @Test
    void testOverloadingMeta() {
        String source = """
            class Test {
                Long x;
                meta n(Long x) { ^ self #new #x(x) }
                meta n(Long x, Long y) { ^ self #new #x(x + y) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("n", 42);
        assertEquals(42, instance.invokeMember("x").asLong());
        instance = meta.invokeMember("n", 40, 2);
        assertEquals(42, instance.invokeMember("x").asLong());
    }

}

