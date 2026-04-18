package tolk.runtime;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/// # JolkString
/// 
/// The runtime representation of the Jolk `String` type definition.
/// 
/// Strings in Jolk are represented by raw `java.lang.String` objects. This class 
/// holds the MetaClass definition (`STRING_TYPE`) and the intrinsic implementations 
/// for string operations like concatenation (`+`).
/// 
public final class JolkStringExtension {

    public static final JolkMetaClass STRING_TYPE;

    static {
        STRING_TYPE = new JolkMetaClass(
            "String", 
            JolkFinality.FINAL, 
            JolkVisibility.PUBLIC, 
            JolkArchetype.CLASS, 
            new HashMap<>(), 
            new HashMap<>()
        );

        Map<String, Object> members = new HashMap<>();
        members.put("+", new StringAdd());
        
        // Object Protocol
        members.put("toString", new StringToString());
        members.put("hash", new StringHash());
        members.put("~~", new StringEquals()); 
        members.put("!~", new StringNotEquals());
        members.put("isPresent", new StringIsPresent());
        members.put("isEmpty", new StringIsEmpty());
        members.put("class", new StringClassAccessor());
        members.put("instanceOf", new StringInstanceOf());
        // Jolk String-specific methods
        members.put("matches", new StringMatches());

        for (var e : members.entrySet()) STRING_TYPE.registerInstanceMethod(e.getKey(), e.getValue());
    }

    private JolkStringExtension() {}

    @ExportLibrary(InteropLibrary.class)
    public static final class StringAdd implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) {
            if (arguments.length < 2) return JolkNothing.INSTANCE;
            InteropLibrary interop = InteropLibrary.getUncached();
            try {
                // Identity Congruence: Reconcile operands to Java Strings
                return interop.asString(arguments[0]) + interop.asString(arguments[1]);
            } catch (com.oracle.truffle.api.interop.UnsupportedMessageException e) {
                return String.valueOf(arguments[0]) + String.valueOf(arguments[1]);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class StringToString implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) { return String.valueOf(arguments[0]); }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class StringHash implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) { 
            return (long) arguments[0].hashCode(); 
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class StringEquals implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) {
            if (arguments.length < 2) return false;
            InteropLibrary interop = InteropLibrary.getUncached();
            try {
                // Identity Congruence: Normalize Guest/Host string representations
                return interop.asString(arguments[0]).equals(interop.asString(arguments[1]));
            } catch (com.oracle.truffle.api.interop.UnsupportedMessageException e) {
                return arguments[0].equals(arguments[1]);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class StringNotEquals implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) {
            Object eq = new StringEquals().execute(arguments);
            return (eq instanceof Boolean b) ? !b : true;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class StringIsPresent implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) { return true; }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class StringIsEmpty implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) { 
            return ((String) arguments[0]).isEmpty(); 
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class StringClassAccessor implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) { return STRING_TYPE; }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class StringInstanceOf implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws UnsupportedMessageException {
            Object receiver = arguments[0];
            Object type = arguments[1];
            if (InteropLibrary.getUncached().isMetaInstance(type, receiver)) {
                return JolkMatch.with(receiver);
            }
            return JolkMatch.empty();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class StringMatches implements TruffleObject {
        @ExportMessage public boolean isExecutable() { return true; }
        @ExportMessage public Object execute(Object[] arguments) throws ArityException {
            if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
            String receiver = (String) arguments[0];
            String regex = String.valueOf(arguments[1]);
            return receiver.matches(regex);
        }
    }
}