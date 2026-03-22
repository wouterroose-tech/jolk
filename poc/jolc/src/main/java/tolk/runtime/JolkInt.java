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
        members.put(">", new GreaterThan());
        members.put("<", new LessThan());
        members.put(">=", new GreaterOrEqual());
        members.put("<=", new LessOrEqual());
        members.put("times", new Times());

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
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            if (arguments[0] instanceof Integer a && arguments[1] instanceof Integer b) {
                return a - b;
            }
            throw UnsupportedTypeException.create(arguments);
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

}
