package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.util.HashMap;
import java.util.Map;

/// # JolkLong
/// 
/// The runtime representation of the Jolk `Long` type definition.
/// 
/// While `Long` values are represented as `java.lang.Long` primitives (or boxed)
/// in the polyglot runtime, this class holds the MetaClass definition (`LONG_TYPE`)
/// and the implementations of the intrinsic operations (addition, comparison, etc.).
///
public final class JolkLongExtension {

    public static final JolkMetaClass LONG_TYPE;

    static {
        // Breaking the circularity: Assign the identity BEFORE populating members
        LONG_TYPE = new JolkMetaClass(
            "Long", 
            JolkFinality.FINAL, 
            JolkVisibility.PUBLIC, 
            JolkArchetype.CLASS, 
            new HashMap<>(), 
            new HashMap<>()
        );

        Map<String, Object> members = new HashMap<>();
        members.put("+", new LongAdd());
        members.put("-", new LongSubtract());
        members.put("*", new LongMultiply());
        members.put("/", new LongDivide());
        members.put("%", new LongModulo());
        members.put("==", new LongEquals());
        members.put("!=", new LongNotEquals());
        members.put(">", new LongGreaterThan());
        members.put("<", new LongLessThan());
        members.put(">=", new LongGreaterOrEqual());
        members.put("<=", new LongLessOrEqual());
        members.put("times", new LongTimes());
        members.put("**", new LongPower());
        // Object Protocol
        members.put("toString", new LongToString());
        members.put("hash", new LongHash());
        members.put("~~", new LongEquals()); // Equivalence defaults to Equality for Long
        members.put("!~", new LongNotEquals());
        members.put("ifPresent", new LongIfPresent());
        members.put("ifEmpty", new LongIfEmpty());
        members.put("isPresent", new LongIsPresent());
        members.put("isEmpty", new LongIsEmpty());
        members.put("class", new LongClassAccessor());
        members.put("instanceOf", new LongInstanceOf());
        // Synthesized Meta-Methods: accessible via instances
        Constant MIN = new Constant(Long.MIN_VALUE);
        Constant MAX = new Constant(Long.MAX_VALUE);
        members.put("MIN", MIN);
        members.put("MAX", MAX);

        Map<String, Object> metaMembers = new HashMap<>();
        metaMembers.put("MIN", MIN);
        metaMembers.put("MAX", MAX);
        
        // Hydrate the existing identity
        for (var e : members.entrySet()) LONG_TYPE.registerInstanceMethod(e.getKey(), e.getValue());
        for (var e : metaMembers.entrySet()) LONG_TYPE.registerMetaMethod(e.getKey(), e.getValue());
    }

    private JolkLongExtension() {
    }

    public static Long asLong(Object arg) {
        if (arg instanceof Long l) return l;
        if (arg instanceof Integer i) return i.longValue();
        return null;
    }

    /// Helper to expose substrate constants as Jolk meta-members.
    @ExportLibrary(InteropLibrary.class)
    public static final class Constant implements TruffleObject {
        private final Object value;
        public Constant(Object value) { this.value = value; }
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            // Allow 0 args (meta-call) or 1 arg (instance-call where arg[0] is the receiver)
            if (arguments.length > 1) throw ArityException.create(0, 1, arguments.length);
            return value;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongAdd implements TruffleObject {
        public LongAdd() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long a = asLong(arguments[0]);
            Long b = asLong(arguments[1]);
            if (a != null && b != null) {
                return a + b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongSubtract implements TruffleObject {
        public LongSubtract() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length == 1) {
                Long a = asLong(arguments[0]);
                if (a != null) {
                    return -a;
                }
                throw UnsupportedTypeException.create(arguments);
            }
            if (arguments.length == 2) {
                Long a = asLong(arguments[0]);
                Long b = asLong(arguments[1]);
                if (a != null && b != null) {
                    return a - b;
                }
                throw UnsupportedTypeException.create(arguments);
            }
            throw ArityException.create(1, 2, arguments.length);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongMultiply implements TruffleObject {
        public LongMultiply() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long a = asLong(arguments[0]);
            Long b = asLong(arguments[1]);
            if (a != null && b != null) {
                return a * b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongDivide implements TruffleObject {
        public LongDivide() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long a = asLong(arguments[0]);
            Long b = asLong(arguments[1]);
            if (a != null && b != null) {
                return a / b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongModulo implements TruffleObject {
        public LongModulo() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long a = asLong(arguments[0]);
            Long b = asLong(arguments[1]);
            if (a != null && b != null) {
                return a % b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongEquals implements TruffleObject {
        public LongEquals() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long a = asLong(arguments[0]);
            Long b = asLong(arguments[1]);
            if (a != null && b != null) {
                return a.equals(b);
            }
            return false;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongNotEquals implements TruffleObject {
        public LongNotEquals() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long a = asLong(arguments[0]);
            Long b = asLong(arguments[1]);
            if (a != null && b != null) {
                return !a.equals(b);
            }
            return true;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongGreaterThan implements TruffleObject {
        public LongGreaterThan() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long a = asLong(arguments[0]);
            Long b = asLong(arguments[1]);
            if (a != null && b != null) {
                return a > b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongLessThan implements TruffleObject {
        public LongLessThan() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long a = asLong(arguments[0]);
            Long b = asLong(arguments[1]);
            if (a != null && b != null) {
                return a < b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongGreaterOrEqual implements TruffleObject {
        public LongGreaterOrEqual() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long a = asLong(arguments[0]);
            Long b = asLong(arguments[1]);
            if (a != null && b != null) {
                return a >= b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongLessOrEqual implements TruffleObject {
        public LongLessOrEqual() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long a = asLong(arguments[0]);
            Long b = asLong(arguments[1]);
            if (a != null && b != null) {
                return a <= b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongTimes implements TruffleObject {
        public LongTimes() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long count = asLong(arguments[0]);
            if (count != null) {
                Object action = arguments[1];
                for (long i = 0; i < count; i++) {
                    InteropLibrary.getUncached().execute(action);
                }
                return count; // Return self
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongPower implements TruffleObject {
        public LongPower() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Long a = asLong(arguments[0]);
            Long b = asLong(arguments[1]);
            if (a != null && b != null) {
                return (long) Math.pow(a, b);
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongToString implements TruffleObject {
        public LongToString() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length); // receiver
            return String.valueOf(arguments[0]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongHash implements TruffleObject {
        public LongHash() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length); // receiver only
            // Explicitly return Long to match the PoC default number type
            return (long) arguments[0].hashCode();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongIfPresent implements TruffleObject {
        public LongIfPresent() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            // arguments[0] is receiver (Int), arguments[1] is action (Closure)
            Object receiver = arguments[0];
            Object action = arguments[1];
            return InteropLibrary.getUncached().execute(action, receiver);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongIfEmpty implements TruffleObject {
        public LongIfEmpty() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            // Int is never empty, do nothing
            return arguments[0]; // Return self
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongClassAccessor implements TruffleObject {
        public LongClassAccessor() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return LONG_TYPE;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongInstanceOf implements TruffleObject {
        public LongInstanceOf() {}
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
    public static final class LongIsPresent implements TruffleObject {
        public LongIsPresent() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            // An Long is always present
            return true;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class LongIsEmpty implements TruffleObject {
        public LongIsEmpty() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            // An Long is never empty
            return false;
        }
    }

}
