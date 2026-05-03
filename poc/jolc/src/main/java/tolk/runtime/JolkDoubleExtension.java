package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ### JolkDoubleExtension
 * 
 * The runtime representation of the Jolk `Double` type definition.
 * 
 * While `Double` values are represented as `java.lang.Double` primitives in the 
 * polyglot runtime, this class holds the MetaClass definition (`DOUBLE_TYPE`)
 * and the implementations of intrinsic floating-point operations.
 */
public class JolkDoubleExtension {

    public static final JolkMetaClass DOUBLE_TYPE;

    static {
        // Breaking the circularity: Assign the identity BEFORE populating members
        DOUBLE_TYPE = new JolkMetaClass(
            "Double", 
            JolkNumberExtension.NUMBER_TYPE, // Identity hierarchy
            JolkFinality.FINAL, 
            JolkVisibility.PUBLIC, 
            JolkArchetype.CLASS,
            java.lang.Double.class // Associate with host class
        );

        Map<String, Object> members = new LinkedHashMap<>();
        members.put("+".intern(), new DoubleAdd());
        members.put("-".intern(), new DoubleSubtract());
        members.put("*".intern(), new DoubleMultiply());
        members.put("/".intern(), new DoubleDivide());
        members.put("%".intern(), new DoubleModulo());
        members.put("**".intern(), new DoublePower());
        members.put("==".intern(), new DoubleEquals());
        members.put("!=".intern(), new DoubleNotEquals());
        members.put(">".intern(), new DoubleGreaterThan());
        members.put("<".intern(), new DoubleLessThan());
        members.put(">=".intern(), new DoubleGreaterOrEqual());
        members.put("<=".intern(), new DoubleLessOrEqual());

        // Object Protocol
        members.put("toString".intern(), new DoubleToString());
        members.put("hash".intern(), new DoubleHash());
        members.put("~~".intern(), new DoubleEquals()); 
        members.put("!~".intern(), new DoubleNotEquals());
        members.put("isPresent".intern(), new DoubleIsPresent());
        members.put("isEmpty".intern(), new DoubleIsEmpty());
        members.put("class".intern(), new DoubleClassAccessor());
        members.put("instanceOf".intern(), new DoubleInstanceOf());
        members.put("round".intern(), new DoubleRound());

        Map<String, Object> metaMembers = new LinkedHashMap<>();
        metaMembers.put("random".intern(), new DoubleRandom());
        metaMembers.put("PI".intern(), Math.PI);

        // Hydrate the existing identity (metaMembers will be populated dynamically via hostClass lookup)
        for (var e : members.entrySet()) DOUBLE_TYPE.registerInstanceMethod(e.getKey(), e.getValue());
        for (var e : metaMembers.entrySet()) DOUBLE_TYPE.registerMetaMethod(e.getKey(), e.getValue());
    }
    
    private JolkDoubleExtension() {
    }

    /**
     * ### asDouble
     * 
     * Performs **Impedance Resolution** to extract a primitive double from 
     * substrate types, supporting **Guided Coercion** from Long/Integer.
     */
    public static double asDouble(Object arg) throws UnsupportedTypeException {
        if (arg instanceof Double d) return d;
        if (arg instanceof Float f) return f.doubleValue();
        if (arg instanceof Long l) return l.doubleValue();
        if (arg instanceof Integer i) return i.doubleValue();
        if (arg instanceof Number n) return n.doubleValue();
        throw UnsupportedTypeException.create(new Object[]{arg});
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleAdd implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            return asDouble(arguments[0]) + asDouble(arguments[1]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleSubtract implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length == 1) return -asDouble(arguments[0]);
            if (arguments.length == 2) return asDouble(arguments[0]) - asDouble(arguments[1]);
            throw ArityException.create(1, 2, arguments.length);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleMultiply implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            return asDouble(arguments[0]) * asDouble(arguments[1]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleDivide implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            return asDouble(arguments[0]) / asDouble(arguments[1]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleModulo implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            return asDouble(arguments[0]) % asDouble(arguments[1]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoublePower implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            return Math.pow(asDouble(arguments[0]), asDouble(arguments[1]));
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleEquals implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            try {
                return asDouble(arguments[0]) == asDouble(arguments[1]);
            } catch (UnsupportedTypeException e) { return false; }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleNotEquals implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            try {
                return asDouble(arguments[0]) != asDouble(arguments[1]);
            } catch (UnsupportedTypeException e) { return true; }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleGreaterThan implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            return asDouble(arguments[0]) > asDouble(arguments[1]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleLessThan implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            return asDouble(arguments[0]) < asDouble(arguments[1]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleGreaterOrEqual implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            return asDouble(arguments[0]) >= asDouble(arguments[1]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleLessOrEqual implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            return asDouble(arguments[0]) <= asDouble(arguments[1]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleToString implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return String.valueOf(arguments[0]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleHash implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            // Jolk Numeric Standard: hash must be returned as a Long
            return (long) Double.valueOf(String.valueOf(arguments[0])).hashCode();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleIsPresent implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) { return true; }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleIsEmpty implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) { return false; }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleClassAccessor implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) { return DOUBLE_TYPE; }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleInstanceOf implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedMessageException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Object receiver = arguments[0];
            Object type = arguments[1];
            if (InteropLibrary.getUncached().isMetaInstance(type, receiver)) {
                return JolkMatch.with(receiver);
            }
            return JolkMatch.empty();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleRound implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return Math.round(asDouble(arguments[0]));
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class DoubleRandom implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            // Jolk Meta-Protocol: receiver (MetaClass) is always arguments[0]
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return Math.random();
        }
    }
}
