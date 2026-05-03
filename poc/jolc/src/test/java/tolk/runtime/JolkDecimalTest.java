package tolk.runtime;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkDecimalTest
 *
 * Verifies the intrinsic behavior of the Jolk `Decimal` type, which is backed by `java.math.BigDecimal`.
 * This includes canonical construction, accessors, rounding, truncation, and conversions.
 */
public class JolkDecimalTest extends JolcTestBase {

    @Test
    void testDecimalLiteral() {
        String source = """
            class DecimalTest {
                Decimal decimal = 123.45;
            }""";
        Value instance = eval(source).invokeMember("new");

        Value decimalValue = instance.invokeMember("decimal");
        assertNotNull(decimalValue);
        assertTrue(decimalValue.isHostObject()); // Still true, as it's a host object
        BigDecimal bd = decimalValue.as(BigDecimal.class);
        assertEquals(new BigDecimal("123.45"), bd);
    }

    @Test
    void testCanonicalNew() {
        String source = """
            class DecimalTest {
                Decimal create(Long significand, Integer scale) {
                    ^ Decimal #new(significand, scale)
                }
            }""";
        Value instance = eval(source).invokeMember("new");

        Value decimalValue = instance.invokeMember("create", 12345L, 2);
        assertNotNull(decimalValue);
        assertTrue(decimalValue.isHostObject());
        BigDecimal bd = decimalValue.as(BigDecimal.class);
        assertEquals(new BigDecimal("123.45"), bd);

        decimalValue = instance.invokeMember("create", -50L, 1);
        bd = decimalValue.as(BigDecimal.class);
        assertEquals(new BigDecimal("-5.0"), bd);
    }

    @Test
    void testSignificandAndScaleAccessors() {
        String source = """
            class DecimalTest {
                Decimal create(Long significand, Integer scale) {
                    ^ Decimal #new(significand, scale)
                }
                Long significand(Decimal d) { ^ d #significand }
                Integer scale(Decimal d) { ^ d #scale }
            }""";
        Value instance = eval(source).invokeMember("new");

        Value decimalValue = instance.invokeMember("create", 12345L, 2);
        assertEquals(12345L, instance.invokeMember("significand", decimalValue).asLong());
        assertEquals(2, instance.invokeMember("scale", decimalValue).asInt());

        decimalValue = instance.invokeMember("create", -50L, 1);
        assertEquals(-50L, instance.invokeMember("significand", decimalValue).asLong());
        assertEquals(1, instance.invokeMember("scale", decimalValue).asInt());
    }

    @Test
    void testRound() {
        String source = """
            class DecimalTest {
                Decimal create(Long significand, Integer scale) {
                    ^ Decimal #new(significand, scale)
                }
                Long round(Decimal d) { ^ d #round }
            }""";
        Value instance = eval(source).invokeMember("new");

        Value d1 = instance.invokeMember("create", 12345L, 2); // 123.45
        assertEquals(123L, instance.invokeMember("round", d1).asLong());

        Value d2 = instance.invokeMember("create", 12350L, 2); // 123.50
        assertEquals(124L, instance.invokeMember("round", d2).asLong()); // HALF_UP rounds .5 up

        Value d3 = instance.invokeMember("create", -12345L, 2); // -123.45
        assertEquals(-123L, instance.invokeMember("round", d3).asLong());

        Value d4 = instance.invokeMember("create", -12350L, 2); // -123.50
        assertEquals(-123L, instance.invokeMember("round", d4).asLong()); // HALF_UP rounds .5 up (towards positive infinity for negative numbers)
    }

    @Test
    void testTruncate() {
        String source = """
            class DecimalTest {
                Decimal create(Long significand, Integer scale) {
                    ^ Decimal #new(significand, scale)
                }
                Long truncate(Decimal d) { ^ d #truncate }
            }""";
        Value instance = eval(source).invokeMember("new");

        Value d1 = instance.invokeMember("create", 12345L, 2); // 123.45
        assertEquals(123L, instance.invokeMember("truncate", d1).asLong());

        Value d2 = instance.invokeMember("create", -12345L, 2); // -123.45
        assertEquals(-123L, instance.invokeMember("truncate", d2).asLong());
    }

    @Test
    void testFraction() {
        String source = """
            class DecimalTest {
                Decimal create(Long significand, Integer scale) {
                    ^ Decimal #new(significand, scale)
                }
                Decimal fraction(Decimal d) { ^ d #fraction }
            }""";
        Value instance = eval(source).invokeMember("new");

        Value d1 = instance.invokeMember("create", 12345L, 2); // 123.45
        Value fraction1 = instance.invokeMember("fraction", d1);
        assertTrue(fraction1.isHostObject());
        assertEquals(new BigDecimal("0.45"), fraction1.as(BigDecimal.class));

        Value d2 = instance.invokeMember("create", -12345L, 2); // -123.45
        Value fraction2 = instance.invokeMember("fraction", d2);
        assertTrue(fraction2.isHostObject());
        assertEquals(new BigDecimal("-0.45"), fraction2.as(BigDecimal.class));

        Value d3 = instance.invokeMember("create", 12300L, 2); // 123.00
        Value fraction3 = instance.invokeMember("fraction", d3);
        assertTrue(fraction3.isHostObject());
        assertEquals(new BigDecimal("0.00"), fraction3.as(BigDecimal.class));
    }

    @Test
    void testAsDouble() {
        String source = """
            class DecimalTest {
                Decimal create(Long significand, Integer scale) {
                    ^ Decimal #new(significand, scale)
                }
                Double asDouble(Decimal d) { ^ d #asDouble }
                Double asDouble() { ^ 123.45 #asDouble }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(123.45, instance.invokeMember("asDouble").asDouble());

        Value d1 = instance.invokeMember("create", 12345L, 2); // 123.45
        assertEquals(123.45, instance.invokeMember("asDouble", d1).asDouble());

        Value d2 = instance.invokeMember("create", -50L, 1); // -5.0
        assertEquals(-5.0, instance.invokeMember("asDouble", d2).asDouble());
    }

    @Test
    void testAsLong() {
        String source = """
            class DecimalTest {
                Decimal create(Long significand, Integer scale) {
                    ^ Decimal #new(significand, scale)
                }
                Long asLong(Decimal d) { ^ d #asLong }
            }""";
        Value instance = eval(source).invokeMember("new");

        Value d1 = instance.invokeMember("create", 12345L, 2); // 123.45
        assertEquals(123L, instance.invokeMember("asLong", d1).asLong());

        Value d2 = instance.invokeMember("create", -12345L, 2); // -123.45
        assertEquals(-123L, instance.invokeMember("asLong", d2).asLong());
    }

    @Test
    void testAsDecimalConversion() {
        String source = """
            class ConversionTest {
                Decimal fromLong(Long l) { ^ l #asDecimal }
                Decimal fromDouble(Double d) { ^ d #asDecimal }
                Decimal fromDecimal(Decimal d) { ^ d #asDecimal }
            }""";
        Value instance = eval(source).invokeMember("new");

        Value decFromLong = instance.invokeMember("fromLong", 123L);
        assertTrue(decFromLong.isHostObject());
        assertEquals(new BigDecimal("123"), decFromLong.as(BigDecimal.class));

        Value decFromDouble = instance.invokeMember("fromDouble", 123.45);
        assertTrue(decFromDouble.isHostObject());
        // Note: BigDecimal.valueOf(double) can have precision issues, but for POC it's fine.
        assertEquals(new BigDecimal("123.45"), decFromDouble.as(BigDecimal.class));

        Value originalDecimal = instance.invokeMember("fromLong", 50L);
        Value decFromDecimal = instance.invokeMember("fromDecimal", originalDecimal);
        assertTrue(decFromDecimal.isHostObject());
        assertEquals(new BigDecimal("50"), decFromDecimal.as(BigDecimal.class));
        assertSame(originalDecimal.as(BigDecimal.class), decFromDecimal.as(BigDecimal.class));
    }
    
    @Test
    void testAritmetic() {
        // Mixing Decimal, Double and Long should trigger automatic promotion to Double (Passive Coercion)
        String source = """
            class CoercionTest {
                Decimal t1() {^ 1.0 / 5 }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(new BigDecimal("0.2"), instance.invokeMember("t1").as(BigDecimal.class));
    }
}