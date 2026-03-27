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
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");  
        assertEquals(42L, instance.invokeMember("val").asLong(), "The field should be initialized to the default value.");
        assertEquals(42L, instance.invokeMember("val2").asLong(), "The field should be initialized to the default value.");
        assertEquals(42L, instance.invokeMember("val3").asLong(), "The field should be initialized to the default value.");
        assertEquals(0L, instance.invokeMember("val4").asLong(), "The field should be initialized to the default value.");
        assertEquals(42L, instance.invokeMember("val5").asLong(), "The field should be initialized to the default value.");
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkNothing.") 
    void testFlowControlMessages() {
        String source = """
            class MinMaxTest {
            Long val() {
                x = 1; 
                null #ifPresent [ x = 2 ];
                ^x }
            }""";
        // #ifPresent should not execute its closure for a null receiver.
        Value ifPresentResult = eval(source);
        assertEquals(1L, ifPresentResult.asLong(), "The #ifPresent block should not execute on null.");

        // #ifEmpty should execute its closure for a null receiver.
        Value ifEmptyResult = eval(source);
        assertEquals(2L, ifEmptyResult.asLong(), "The #ifEmpty block should execute on null.");
    }

}
