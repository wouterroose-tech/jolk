package tolk.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/// # JolkClosure (The Reified Identity)
/// 
/// A first-class identity representing a block of deferred logic. Unlike traditional 
/// procedural callbacks, a Jolk Closure maintains **Scope Permeability**, retaining 
/// a link to its lexical environment via a {@link MaterializedFrame}.
/// 
/// Depending on the receiving selector, a closure interacts with its environment 
/// via **Inline Projection** (for control flow) or **Unbounded Projection** (for 
/// functional passing), maintaining **Return Authority** via the caret terminal.
/// 
/// It also serves as the foundation for **Resource Governance**. When acting as a 
/// receiver for the `#try` message, the closure functions as a factory for an 
/// **Atomic Resource**, ensuring that the resulting identity is bound by a 
/// guaranteed cleanup protocol.
/// 
@ExportLibrary(InteropLibrary.class)
public class JolkClosure implements TruffleObject {
    private final CallTarget callTarget;
    private final MaterializedFrame environment;

    public JolkClosure(CallTarget callTarget) {
        this.callTarget = callTarget;
        this.environment = null;
    }

    public JolkClosure(CallTarget callTarget, MaterializedFrame environment) {
        this.callTarget = callTarget;
        this.environment = environment;
    }

    /**
     * ### getCallTarget
     * 
     * Provides access to the executable target for optimized execution via 
     * {@link com.oracle.truffle.api.nodes.IndirectCallNode}.
     */
    public CallTarget getCallTarget() {
        return callTarget;
    }

    /**
     * ### getEnvironment
     * 
     * Returns the captured lexical environment. This is passed as the first 
     * argument to the call target during execution.
     */
    public MaterializedFrame getEnvironment() {
        return environment;
    }

    @Override
    public String toString() {
        return "[Closure]";
    }

    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @ExportMessage
    public Object execute(Object[] arguments) {
        Object env = this.environment;
        if (env != null) {
            // Prepend the captured environment (lexical scope) at index 0
            Object[] captures = new Object[arguments.length + 1];
            captures[0] = env;
            System.arraycopy(arguments, 0, captures, 1, arguments.length);
            
            return lift(callTarget.call(captures));
        }
        // Method mode: the receiver is already at index 0
        return lift(callTarget.call(arguments));
    }


    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal) {
        return new JolkMemberNames(new String[]{"apply", "catch", "finally", "try"});
    }

    @ExportMessage
    public boolean isMemberInvocable(String member) {
        return switch (member) {
            case "apply", "catch", "finally", "try" -> true;
            default -> false;
        };
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        switch (member) {
            case "apply":
                return execute(arguments);
            case "catch":
                if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                Object errorType = arguments[0];
                Object handler = arguments[1];
                try {
                    return execute(new Object[0]);
                } catch (RuntimeException | Error t) {
                    if (errorType instanceof Class<?> clazz) {
                         if (clazz.isInstance(t)) return InteropLibrary.getUncached().execute(handler, t);
                    } else if (InteropLibrary.getUncached().isMetaInstance(errorType, t)) {
                         return InteropLibrary.getUncached().execute(handler, t);
                    }
                    throw t;
                }
            case "finally":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                Object finalAction = arguments[0];
                try {
                    return execute(new Object[0]);
                } finally {
                    InteropLibrary.getUncached().execute(finalAction);
                }
            case "try":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                Object logic = arguments[0];
                Object resource = execute(new Object[0]);
                Throwable thrown = null;
                try {
                    return InteropLibrary.getUncached().execute(logic, resource);
                } catch (Throwable t) {
                    thrown = t;
                    throw t;
                } finally {
                    closeResource(resource, thrown);
                }
            default:
                throw UnknownIdentifierException.create(member);
        }
    }

    @TruffleBoundary
    private static void closeResource(Object resource, Throwable prior) {
        if (resource == null || resource == JolkNothing.INSTANCE) return;
        
        InteropLibrary interop = InteropLibrary.getUncached();
        Object unwrapped = unwrap(resource);
        
        Throwable closeFailure = null;
        try {
            // 1. Host Interface Priority: Direct call to AutoCloseable if it's a Java object
            if (unwrapped instanceof AutoCloseable ac) {
                ac.close();
            } 
            // 2. Guest/Interop Protocol: Invoke #close if defined on the object
            else if (interop.isMemberInvocable(resource, "close")) {
                interop.invokeMember(resource, "close");
            }
        } catch (Throwable t) {
            closeFailure = t;
        }

        if (closeFailure != null) {
            if (prior != null) {
                prior.addSuppressed(closeFailure);
            } else {
                throw new RuntimeException("Failed to close resource", closeFailure);
            }
        }
    }

    @TruffleBoundary
    private static Object lift(Object value) {
        if (value == null || value == JolkNothing.INSTANCE) return JolkNothing.INSTANCE;
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character || value instanceof TruffleObject) {
            return value;
        }
        // Identity Restitution: Ensure host objects are wrapped for Interop consistency.
        return tolk.language.JolkLanguage.getContext().env.asGuestValue(value);
    }

    @TruffleBoundary
    private static Object unwrap(Object value) {
        var env = tolk.language.JolkLanguage.getContext().env;
        if (env != null && env.isHostObject(value)) {
            return env.asHostObject(value);
        }
        return value;
    }
}