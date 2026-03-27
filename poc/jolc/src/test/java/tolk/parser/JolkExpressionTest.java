package tolk.parser;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.*;

/// ## JolkExpressionNothingTest
///
///
public class JolkExpressionTest extends JolcTestBase {

    private Value eval(String className, String source) {
        Value result = eval(source);
        assertFalse(result.isNull());
        assertTrue(result.isMetaObject());
        assertEquals(className, result.getMetaQualifiedName());
        return result;
    }

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
    }

    @Test
    @Disabled("Pending implementation of assignments") 
    void testFlowControlMessages() {
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
    @Disabled("binding not yet implemented for the PoC.")
    void testControlFlow() {
        String source = """
            class FlowTest {
                Int check(Boolean b) {
                    res = 0;
                    b ? [ res = 1 ] : [ res = 2 ];
                    ^ res
                }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertEquals(1L, instance.invokeMember("check", true).asLong());
        assertEquals(2L, instance.invokeMember("check", false).asLong());
    }

}
