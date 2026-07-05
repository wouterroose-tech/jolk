package tolk.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkSelfReturnTest extends JolcTestBase {
    
    /// the self-return contract
    @Test
    void test_1() {
        String source = """
            class TestClass {
                Long x;
                y() { self #x(10) }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(instance, instance.invokeMember("y"));
    }
    
    @Test
    void test_2() {
        String source = """
            class TestClass {
                Array<String> nodes = #[];
                add() { self #nodes #add("ok") }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(instance, instance.invokeMember("add"));
    }
}
