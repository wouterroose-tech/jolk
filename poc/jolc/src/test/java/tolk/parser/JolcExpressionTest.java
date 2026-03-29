package tolk.parser;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.*;

/// ## JolkExpressionNothingTest
///
///
public class JolcExpressionTest extends JolcTestBase {

    @Test
    void testExpression() {
        String source = """
            class MinMaxTest {
            Long val() { ^ 42 }
            Long val2() { ^ 40 + 2 }
            Long val3() { ^ true ? 42 : 0 }
            Long val4() { ^ false ? 42 : 0 }
            Long val5() { ^ null ?? 42 }
            Long val6() { ^ (2 * 1) + (20 * 2) }
            Long val7() { ^ (2 ** 2) + (19 * 2) }
            Long val8() { ^ 1 == 2 ? 1 : 0 }
            Long val9() { ^ 1 == 1 ? 1 : 0 }
            Long val10() { ^ 1 != 1 ? 1 : 0 }
            Long val11() { ^ 1 != 2 ? 1 : 0 }
            Long val12() { ^ true ?! 42 : 0}
            Long val13() { ^ false ?! 42 : 0}
            Long val14() { true ? [^42] : [^0] }
            Long val15() { true ?! [^42] : [^0] }
            Long val16() { false ? [^42] : [^0] }
            Long val17() { false ?! [^42] : [^0] }
            Long val18() { ^ 1 < 2 ? 42 : 0 }
            Long val19() { ^ 1 > 2 ? 42 : 0 }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");  
        assertEquals(42L, instance.invokeMember("val").asLong());
        assertEquals(42L, instance.invokeMember("val2").asLong());
        assertEquals(42L, instance.invokeMember("val3").asLong());
        assertEquals(0L, instance.invokeMember("val4").asLong());
        assertEquals(42L, instance.invokeMember("val5").asLong());
        assertEquals(42L, instance.invokeMember("val6").asLong());
        assertEquals(42L, instance.invokeMember("val7").asLong());
        assertEquals(0L, instance.invokeMember("val8").asLong());
        assertEquals(1L, instance.invokeMember("val9").asLong());
        assertEquals(0L, instance.invokeMember("val10").asLong());
        assertEquals(1L, instance.invokeMember("val11").asLong());
        assertEquals(0L, instance.invokeMember("val12").asLong());
        assertEquals(42L, instance.invokeMember("val13").asLong());
        assertEquals(42L, instance.invokeMember("val14").asLong());
        assertEquals(0L, instance.invokeMember("val15").asLong());
        assertEquals(0L, instance.invokeMember("val16").asLong());
        assertEquals(42L, instance.invokeMember("val17").asLong());
        assertEquals(42L, instance.invokeMember("val18").asLong());
        assertEquals(0L, instance.invokeMember("val19").asLong());
    }

    @Test
    void testControlFlow() {
        String sourceIfPresent = """
            class FlowPresentTest {
                Long x;
                Long val() {
                    x = 1; 
                    null #ifPresent [ x = 2 ];
                    ^x }
            }""";
        Value meta1 = eval(sourceIfPresent);
        Value instance1 = meta1.invokeMember("new");
        assertEquals(1L, instance1.invokeMember("val").asLong(), "The #ifPresent block should not execute on null.");

        String sourceIfEmpty = """
            class FlowEmptyTest {
                Long x;
                Long val() {
                    x = 1; 
                    null #ifEmpty [ x = 2 ];
                    ^x }
            }""";
        Value meta2 = eval(sourceIfEmpty);
        Value instance2 = meta2.invokeMember("new");
        assertEquals(2L, instance2.invokeMember("val").asLong(), "The #ifEmpty block should execute on null.");
    }

    @Test
    void testControlFlow_1() {
        String source = """
            class FlowTest {
                Int check(Boolean b) {
                    Int res = 0;
                    b ? [ res = 1 ] : [ res = 2 ];
                    ^ res
                }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertEquals(1L, instance.invokeMember("check", true).asLong());
        assertEquals(2L, instance.invokeMember("check", false).asLong());
    }

    @Test
    void testControlFlow_2() {
        String source = """
            class MyClass {
                Long ifPresentTrue() { Long x = 21; x #ifPresent [v -> x = v * 2 ]; ^x }
                Long ifPresentFalse() { Long x = 42; null #ifPresent [v -> x = 0 ]; ^x }
                Long ifEmptyTrue() { Long x = 0; null #ifEmpty [ x = 42 ]; ^x }
                Long ifEmptyFalse() { Long x = 42; x #ifEmpty [ x = 0 ]; ^x }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");  

        assertEquals(42L, instance.invokeMember("ifPresentTrue").asLong());
        assertEquals(42L, instance.invokeMember("ifPresentFalse").asLong());
        assertEquals(42L, instance.invokeMember("ifEmptyTrue").asLong());
        assertEquals(42L, instance.invokeMember("ifEmptyFalse").asLong());
    }

    /**
     * ### testCoreProtocol
     * 
     * Verifies the Jolk Object Foundation: #hash, #toString, and #instanceOf.
     */
    @Test
    void testCoreProtocol() {
        String source = """
            class ProtocolTest {
                Long getHash(Object o) { ^ o #hash }
                String asString(Object o) { ^ o #toString }
            }""";
        Value instance = eval(source).invokeMember("new");

        assertEquals(42, instance.invokeMember("getHash", 42L).asInt());
        assertEquals("42", instance.invokeMember("asString", 42L).asString());
    }

    /**
     * ### testNothingSilentAbsorption
     * 
     * Verifies that 'null' (Nothing) safely absorbs messages without crashing,
     * supporting the "Neutral Response" model.
     */
    @Test
    void testNothingSilentAbsorption() {
        String source = """
            class NothingTest {
                Object run() { ^ null #someArbitraryMessage #anotherOne }
            }""";
        Value result = eval(source).invokeMember("new").invokeMember("run");
        
        // JolkNothing is a first-class identity, not a polyglot null.
        assertEquals("null", result.toString(), "null should absorb messages and return the 'null' identity string.");
    }

}
