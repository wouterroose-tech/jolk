package tolk.runtime;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkObjectTest
 *
 * Validates the core protocol defined in `jolk.lang.Object`, which serves as the
 * root for all objects in the Jolk ecosystem. These tests ensure that fundamental
 * behaviors like identity, equivalence, and flow control are correctly implemented.
 */
public class JolkObjectTest extends JolcTestBase {

    @Test
    @Disabled("Pending implementation of the core protocol in JolkObject.") 
    void testIdentityOperators() {
        String source = """
            class TestObj {}
            x = TestObj #new;
            y = TestObj #new;
            #(
                "self_identity" -> (x == x),
                "other_identity" -> (x == y)
            )
        """;
        Value results = eval(source);
        assertTrue(results.getMember("self_identity").asBoolean(), "An object must be identical to itself.");
        assertFalse(results.getMember("other_identity").asBoolean(), "Two distinct objects should not be identical.");
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkObject.") 
    void testDefaultEquivalenceIsIdentity() {
        String source = """
            class TestObj {}
            x = TestObj #new;
            y = TestObj #new;
            #(
                "self_equiv" -> (x ~~ x),
                "other_equiv" -> (x ~~ y)
            )
        """;
        Value results = eval(source);
        // By default, equivalence (~~) should fall back to identity (==).
        assertTrue(results.getMember("self_equiv").asBoolean(), "An object must be equivalent to itself.");
        assertFalse(results.getMember("other_equiv").asBoolean(), "Two distinct objects should not be equivalent by default.");
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkObject.") 
    void testOverriddenEquivalence() {
        // Define a class that overrides the equivalence operator '~~'.
        String source = """
            class Point {
                Int x;
                Int y;

                Boolean ~~(Object other) {
                    (self == other) ? [ ^true ];
                    other #as(Point) #ifPresent [ p ->
                        ^ (self #x == p #x) && (self #y == p #y)
                    ];
                    ^ false
                }
            }

            p1 = Point #new; p1 #x(10); p1 #y(20);
            p2 = Point #new; p2 #x(10); p2 #y(20);
            p3 = Point #new; p3 #x(0); p3 #y(0);

            #( "p1_vs_p2" -> (p1 ~~ p2), "p1_vs_p3" -> (p1 ~~ p3) )
        """;
        Value results = eval(source);
        assertTrue(results.getMember("p1_vs_p2").asBoolean(), "Two points with the same coordinates should be equivalent.");
        assertFalse(results.getMember("p1_vs_p3").asBoolean(), "Two points with different coordinates should not be equivalent.");
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkObject.") 
    void testFlowControlMessagesOnObject() {
        // The #ifPresent block should execute for a valid object.
        Value ifPresentResult = eval("class O{} x = 1; obj = O #new; obj #ifPresent [ x = 2 ]; ^x");
        assertEquals(2, ifPresentResult.asInt(), "The #ifPresent block should execute on a non-null object.");

        // The #ifEmpty block should not execute.
        Value ifEmptyResult = eval("class O{} x = 1; obj = O #new; obj #ifEmpty [ x = 2 ]; ^x");
        assertEquals(1, ifEmptyResult.asInt(), "The #ifEmpty block should not execute on a non-null object.");
    }

}
