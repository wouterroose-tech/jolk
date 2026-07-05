package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkFluidMessagingTest extends JolcTestBase {

    @Test
    void testMessageChain() {
        String source = """
            class MyClass {
                a() { ^ self }
                b() { ^ self }
                Long c() { ^ 42 }
                Long run() { ^ self #a() #b() #c() }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("run").asLong());
    }

    @Test
    void testMessageChain_2() {
        String source = """
            class MyClass {
                a() { ^ self }
                b() { ^ self }
                Long c(Long x) { ^ x }
                Long run() { ^ self #a() #b() #c(42) }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(42, instance.invokeMember("run").asLong());
    }

}
