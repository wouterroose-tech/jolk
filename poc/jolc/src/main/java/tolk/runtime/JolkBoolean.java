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

/// # JolkBoolean
/// 
/// The runtime representation of the Jolk `Boolean` type definition.
/// 
/// In the Jolk runtime, boolean identities are represented by Java `boolean` 
/// primitives. This class holds the MetaClass definition (`BOOLEAN_TYPE`) 
/// and the implementations for the intrinsic logical operations and 
/// control-flow messages.
///
public final class JolkBoolean {

    public static final JolkMetaClass BOOLEAN_TYPE;

    static {
        Map<String, Object> members = new HashMap<>();
        members.put("&&", new BooleanAnd());
        members.put("||", new BooleanOr());
        members.put("!", new BooleanNot());
        members.put("?", new BooleanIfTrue());
        members.put("?!", new BooleanIfFalse());
        members.put(":", new BooleanElse());
        // Object Protocol
        members.put("==", new BooleanEquals());
        members.put("!=", new BooleanNotEquals());
        members.put("~~", new BooleanEquals());
        members.put("!~", new BooleanNotEquals());
        members.put("toString", new BooleanToString());
        members.put("hash", new BooleanHash());
        members.put("ifPresent", new BooleanIfPresent());
        members.put("ifEmpty", new BooleanIfEmpty());
        members.put("isPresent", new BooleanIsPresent());
        members.put("isEmpty", new BooleanIsEmpty());
        members.put("class", new BooleanClassAccessor());
        members.put("instanceOf", new BooleanInstanceOf());

        BOOLEAN_TYPE = new JolkMetaClass("Boolean", JolkFinality.FINAL, JolkVisibility.PUBLIC, JolkArchetype.CLASS, members, Map.of());
    }

    private JolkBoolean() {
    }

    static Boolean asBoolean(Object arg) {
        if (arg instanceof Boolean b) return b;
        return null;
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanAnd implements TruffleObject {
        public BooleanAnd() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Boolean a = asBoolean(arguments[0]);
            Boolean b = asBoolean(arguments[1]);
            if (a != null && b != null) return a && b;
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanOr implements TruffleObject {
        public BooleanOr() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Boolean a = asBoolean(arguments[0]);
            Boolean b = asBoolean(arguments[1]);
            if (a != null && b != null) return a || b;
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanNot implements TruffleObject {
        public BooleanNot() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            Boolean a = asBoolean(arguments[0]);
            if (a != null) return !a;
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanIfTrue implements TruffleObject {
        public BooleanIfTrue() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Boolean receiver = asBoolean(arguments[0]);
            Object action = arguments[1];
            if (receiver != null) {
                if (receiver) InteropLibrary.getUncached().execute(action, receiver);
                return receiver;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanIfFalse implements TruffleObject {
        public BooleanIfFalse() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Boolean receiver = asBoolean(arguments[0]);
            Object action = arguments[1];
            if (receiver != null) {
                if (!receiver) InteropLibrary.getUncached().execute(action, receiver);
                return receiver;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanElse implements TruffleObject {
        public BooleanElse() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            // In a fluent chain, ":" behaves like "ifFalse" 
            Boolean receiver = asBoolean(arguments[0]);
            Object action = arguments[1];
            if (receiver != null) {
                if (!receiver) InteropLibrary.getUncached().execute(action, receiver);
                return receiver;
            }
            throw UnsupportedTypeException.create(arguments);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanEquals implements TruffleObject {
        public BooleanEquals() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Boolean a = asBoolean(arguments[0]);
            Boolean b = asBoolean(arguments[1]);
            if (a != null && b != null) return a.equals(b);
            return false;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanNotEquals implements TruffleObject {
        public BooleanNotEquals() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Boolean a = asBoolean(arguments[0]);
            Boolean b = asBoolean(arguments[1]);
            if (a != null && b != null) return !a.equals(b);
            return true;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanToString implements TruffleObject {
        public BooleanToString() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return String.valueOf(arguments[0]);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanHash implements TruffleObject {
        public BooleanHash() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return (long) arguments[0].hashCode();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanIfPresent implements TruffleObject {
        public BooleanIfPresent() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            Object receiver = arguments[0];
            Object action = arguments[1];
            InteropLibrary.getUncached().execute(action, receiver);
            return receiver;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanIfEmpty implements TruffleObject {
        public BooleanIfEmpty() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            return arguments[0]; // Boolean is never empty
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanIsPresent implements TruffleObject {
        public BooleanIsPresent() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return true;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanIsEmpty implements TruffleObject {
        public BooleanIsEmpty() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return false;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanClassAccessor implements TruffleObject {
        public BooleanClassAccessor() {}
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
            return BOOLEAN_TYPE;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class BooleanInstanceOf implements TruffleObject {
        public BooleanInstanceOf() {}
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

}
