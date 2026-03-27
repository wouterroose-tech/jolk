package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.util.Collections;
import java.util.Map;

/// # JolkNothing
/// 
/// Represents the `null` or `Nothing` value in Jolk.
///
/// This is a singleton `TruffleObject` that aligns with Jolk's philosophy of treating
/// `null` as a first-class identity. It does NOT implement `isNull` from `InteropLibrary`
/// to ensure it remains a valid receiver for messages within the polyglot environment.
///
@ExportLibrary(InteropLibrary.class)
public final class JolkNothing implements TruffleObject {

    /// The singleton instance of the `Nothing` value.
    public static final JolkNothing INSTANCE = new JolkNothing();

    public static final JolkMetaClass NOTHING_TYPE;

    static {
        Map<String, Object> members = Collections.singletonMap("new", new NothingNew());
        NOTHING_TYPE = new JolkMetaClass("Nothing", JolkFinality.FINAL, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), members);
    }

    private JolkNothing() {
        // private constructor to enforce singleton pattern
    }

    @ExportMessage
    public String toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return "null";
    }

    /**
     * ### isBoolean
     * 
     * Identifies the `Nothing` identity as a boolean-compatible value 
     * to support Jolk's "Neutral Response" model.
     */
    @ExportMessage
    public boolean isBoolean() {
        return true;
    }

    /**
     * ### asBoolean
     * 
     * Returns `false` for the `Nothing` identity, ensuring that 
     * uninitialized states or absent results act as falsy signals 
     * in logical contexts.
     */
    @ExportMessage
    public boolean asBoolean() {
        return false;
    }

    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal) {
        return new JolkMemberNames(new String[]{"~~", "!~", "hash", "toString", "isPresent", "isEmpty", "ifPresent", "ifEmpty", "class", "instanceOf"});
    }

    @ExportMessage
    public boolean isMemberInvocable(String member) {
        // Silent Absorption: JolkNothing accepts all messages.
        return true;
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        switch (member) {
            case "~~":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return this == arguments[0];
            case "!~":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return this != arguments[0];
            case "hash":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return 0L;
            case "toString":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return "null";
            case "isPresent":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return false;
            case "isEmpty":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return true;
            case "ifPresent":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                // Action is ignored for Nothing
                return this;
            case "ifEmpty":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return InteropLibrary.getUncached().execute(arguments[0]);
            case "instanceOf":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);                
                Object type = arguments[0];
                if (InteropLibrary.getUncached().isMetaInstance(type, this)) {
                    return JolkMatch.with(this);
                }
                return JolkMatch.empty();
            case "class":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return NOTHING_TYPE;
            default:
                // Silent Absorption: Return self for any unknown message
                return this;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class NothingNew implements TruffleObject {
        @ExportMessage
        public boolean isExecutable() {
            return true;
        }
        @ExportMessage
        public Object execute(Object[] arguments) {
            throw new RuntimeException("Nothing cannot be instantiated. Use 'null' literal.");
        }
    }
}