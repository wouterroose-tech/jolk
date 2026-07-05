package tolk.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkOverloadingTest extends JolcTestBase {


    @Test
    void testOverloading() {
        String source = """
            class Test {
                Long x() { ^ 0 }
                Long x(Long x) { ^ x }
            }""";
        eval(source);
        Value instance = eval(source).invokeMember("new");
        assertEquals(0L, instance.invokeMember("x").asLong());
        assertEquals(42L, instance.invokeMember("x", 42L).asLong());
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
                #> Long x(DomainClass d) { ^ self #x(d #x) }
                Long x(Long x) { ^ x }
                Long x() { ^ self #x(DomainClass #new) }
                Long test() { ^ self #x }
            }""";
        eval(source);
        Value instance = eval(source).invokeMember("new");
        assertEquals(42L, instance.invokeMember("x").asLong());
        assertEquals(42L, instance.invokeMember("x", 42L).asLong());
        assertEquals(42L, instance.invokeMember("test").asLong());
    }


}
