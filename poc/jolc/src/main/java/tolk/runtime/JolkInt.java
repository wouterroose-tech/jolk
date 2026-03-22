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

/// # JolkInt
/// 
/// The runtime representation of the Jolk `Int` type definition.
/// 
/// While `Int` values are represented as `java.lang.Integer` primitives (or boxed)
/// in the polyglot runtime, this class holds the MetaClass definition (`INT_TYPE`)
/// and the implementations of the intrinsic operations (addition, comparison, etc.).
///
public final class JolkInt {

    public static final JolkMetaClass INT_TYPE;

    static {
        Map<String, Object> members = new HashMap<>();
        members.put("+", new Add());
        members.put("-", new Subtract());
        members.put("*", new Multiply());
        members.put("/", new Divide());
        members.put("%", new Modulo());
        members.put("==", new Equals());
        members.put("!=", new NotEquals());
        members.put(">", new GreaterThan());
        members.put("<", new LessThan());
        members.put(">=", new GreaterOrEqual());
        members.put("<=", new LessOrEqual());
        members.put("times", new Times());
        members.put("**", new Power());
        // Object Protocol
        members.put("toString", new ToString());
        members.put("hash", new Hash());
        members.put("~~", new Equals()); // Equivalence defaults to Equality for Int
        members.put("!~", new NotEquals());
        members.put("ifPresent", new IfPresent());
        members.put("ifEmpty", new IfEmpty());
        members.put("isPresent", new IsPresent());
        members.put("isEmpty", new IsEmpty());
        members.put("class", new ClassAccessor());
        members.put("instanceOf", new InstanceOf());

        INT_TYPE = new JolkMetaClass("Int", JolkFinality.FINAL, JolkVisibility.PUBLIC, JolkArchetype.CLASS, members);
    }

    private JolkInt() {
    }

    @ExportLibrary(InteropLibrary.class)
    static final class Add implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return a + b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class Subtract implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length == 1) {
                if (arguments[0] instanceof Integer a) {
                    return -a;
                }
                throw UnsupportedTypeException.create(arguments);
            }
            if (arguments.length == 2) {
                if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                    return a - b;
                }
                throw UnsupportedTypeException.create(arguments);
            }
            throw ArityException.create(1, 2, arguments.length);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class Multiply implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return a * b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class Divide implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return a / b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class Modulo implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return a % b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class Equals implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return a.equals(b);
            }
            return false;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class NotEquals implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return !a.equals(b);
            }
            return true;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class GreaterThan implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return a > b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class LessThan implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return a < b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class GreaterOrEqual implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return a >= b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class LessOrEqual implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return a <= b;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class Times implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer count) {
                Object action = arguments[1];
                for (int i = 0; i < count; i++) {
                    InteropLibrary.getUncached().execute(action);
                }
                return count; // Return self
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class Power implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return (int) Math.pow(a, b);
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class ToString implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length); // receiver
            return String.valueOf(arguments[0]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class Hash implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(0, 0, arguments.length - 1); // receiver only
            return arguments[0].hashCode();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class IfPresent implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            // arguments[0] is receiver (Int), arguments[1] is action (Closure)
            Object receiver = arguments[0];
            Object action = arguments[1];
            InteropLibrary.getUncached().execute(action, receiver);
            return receiver; // Return self
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class IfEmpty implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            // Int is never empty, do nothing
            return arguments[0]; // Return self
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class ClassAccessor implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(0, 0, arguments.length - 1);
            return INT_TYPE;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class InstanceOf implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException, UnsupportedMessageException {
            if (arguments.length != 2) throw ArityException.create(1, 1, arguments.length - 1);
            Object receiver = arguments[0];
            Object type = arguments[1];
            if (InteropLibrary.getUncached().isMetaInstance(type, receiver)) {
                return JolkMatch.with(receiver);
            }
            return JolkMatch.empty();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class IsPresent implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(0, 0, arguments.length - 1);
            // An Integer is always present
            return true;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class IsEmpty implements TruffleObject {
        @ExportMessage boolean isExecutable() { return true; }
        @ExportMessage Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(0, 0, arguments.length - 1);
            // An Integer is never empty
            return false;
        }
    }

}
