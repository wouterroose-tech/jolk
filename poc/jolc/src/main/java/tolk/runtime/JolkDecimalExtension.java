package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import tolk.nodes.JolkNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * ### JolkDecimalExtension
 * 
 * The runtime representation of the Jolk `Decimal` type definition.
 */
public class JolkDecimalExtension {

    public static final JolkMetaClass DECIMAL_TYPE;

    static {
        DECIMAL_TYPE = new JolkMetaClass(
            "Decimal", 
            JolkNumberExtension.NUMBER_TYPE, 
            JolkFinality.FINAL, 
            JolkVisibility.PUBLIC, 
            JolkArchetype.CLASS,
            java.math.BigDecimal.class
        );

        Map<String, Object> members = new LinkedHashMap<>();
        members.put("significand".intern(), new DecimalSignificand());
        members.put("scale".intern(), new DecimalScale());
        members.put("round".intern(), new DecimalRound());
        members.put("truncate".intern(), new DecimalTruncate());
        members.put("fraction".intern(), new DecimalFraction());
        members.put("asDouble".intern(), new DecimalAsDouble());
        members.put("asLong".intern(), new DecimalAsLong());

        Map<String, Object> metaMembers = new LinkedHashMap<>();
        metaMembers.put("new".intern(), new DecimalFactory());

        for (var e : members.entrySet()) DECIMAL_TYPE.registerInstanceMethod(e.getKey(), e.getValue());
        for (var e : metaMembers.entrySet()) DECIMAL_TYPE.registerMetaMethod(e.getKey(), e.getValue());
    }

    @ExportLibrary(InteropLibrary.class)
    /**
     * ### DecimalNew
     * 
     * The meta-level factory for the Decimal identity. It accepts a significand 
     * and a scale, projecting them into a `java.math.BigDecimal` substrate.
     */
    public static final class DecimalFactory implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 3) throw ArityException.create(2, 2, arguments.length - 1);
            long sig = JolkLongExtension.asLong(arguments[1]);
            int scale = (int) JolkLongExtension.asLong(arguments[2]);
            BigInteger unscaled = BigInteger.valueOf(sig);
            return JolkNode.interopLift(new BigDecimal(unscaled, scale));
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DecimalSignificand implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            BigDecimal d = (BigDecimal) JolkNode.unwrap(arguments[0]);
            return JolkNode.interopLift(d.unscaledValue().longValue());
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DecimalScale implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            BigDecimal d = (BigDecimal) JolkNode.unwrap(arguments[0]);
            return JolkNode.interopLift((long) d.scale()); // Return as Jolk Long (Integer in Jolk is Long in Java)
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DecimalRound implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            BigDecimal d = (BigDecimal) JolkNode.unwrap(arguments[0]);
            // To ensure consistency with Double #round (which uses Math.round) and satisfy 
            // the Identity Congruence principle, we round .5 towards positive infinity.
            RoundingMode mode = (d.signum() >= 0) ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN;
            return JolkNode.interopLift(d.setScale(0, mode).longValue());
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DecimalTruncate implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return JolkNode.interopLift(((BigDecimal) JolkNode.unwrap(arguments[0])).longValue());
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DecimalFraction implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            BigDecimal bd = (BigDecimal) JolkNode.unwrap(arguments[0]);
            BigDecimal fraction = bd.remainder(BigDecimal.ONE); // Correctly operate on BigDecimal
            return JolkNode.interopLift(fraction);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DecimalAsDouble implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return JolkNode.interopLift(((BigDecimal) JolkNode.unwrap(arguments[0])).doubleValue());
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DecimalAsLong implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return JolkNode.interopLift(((BigDecimal) JolkNode.unwrap(arguments[0])).longValue());
        }
    }
}