package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * ### JolkBuiltinMethod
 * 
 * A substrate object representing a method implemented in native Java logic.
 * It implements the Truffle Interop `execute` message to allow seamless 
 * integration with the Jolk dispatch system.
 */
@ExportLibrary(InteropLibrary.class)
public abstract class JolkBuiltinMethod implements TruffleObject {

    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    /**
     * ### execute
     * 
     * Executes the built-in logic. 
     * 
     * @param arguments The call arguments. Index 0 is always the receiver.
     * @return The result of the execution.
     * @throws ArityException If the argument count is incorrect.
     * @throws UnsupportedTypeException If the argument types are invalid.
     */
    @ExportMessage
    public abstract Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException;

    /**
     * ### lift
     * 
     * Performs **Identity Restitution**. Converts raw JVM nulls to `JolkNothing` 
     * and ensures raw host objects are wrapped for Truffle Interop safety.
     */
    protected static Object lift(Object value) {
        if (value == null) return JolkNothing.INSTANCE;
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character || value instanceof TruffleObject) {
            return value;
        }
        // Identity Restitution: Wrap raw Java objects as Host Objects for Interop safety.
        return tolk.language.JolkLanguage.getContext().env.asGuestValue(value);
    }

    /**
     * ### unwrap
     * 
     * Performs **Impedance Resolution**. If the provided value is a wrapped 
     * Truffle Host Object, it extracts the underlying Java instance.
     */
    protected static Object unwrap(Object value) {
        var env = tolk.language.JolkLanguage.getContext().env;
        if (env.isHostObject(value)) {
            return env.asHostObject(value);
        }
        return value;
    }
}