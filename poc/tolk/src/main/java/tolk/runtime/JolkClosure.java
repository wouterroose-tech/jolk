package tolk.runtime;

import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

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

import tolk.nodes.JolkNode;

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
    private MaterializedFrame environment;

    private String methodReferenceSelector;
    private Object methodReferenceCapturedReceiver;
    private int methodReferenceExpectedArity;

    public JolkClosure(CallTarget callTarget) {
        this(callTarget, null, false, null, null, 0);
    }

    public JolkClosure(CallTarget callTarget, MaterializedFrame environment) {
        this(callTarget, environment, false, null, null, 0);
    }

    public JolkClosure(CallTarget callTarget, MaterializedFrame environment,
                   boolean isMethodReference, Object methodCapturedReceiver,
                   String methodSelector, int methodExpectedArity) {
        this.callTarget = callTarget;
        this.environment = environment;
        // Normalize into the canonical methodReference* fields
        if (isMethodReference) {
            this.methodReferenceCapturedReceiver = methodCapturedReceiver;
            this.methodReferenceSelector = methodSelector;
            this.methodReferenceExpectedArity = methodExpectedArity;
        }
    }

    public JolkClosure(CallTarget callTarget, String methodReferenceSelector, Object methodReferenceCapturedReceiver, int methodReferenceExpectedArity) {
        this.callTarget = callTarget;
        this.methodReferenceSelector = methodReferenceSelector;
        this.methodReferenceCapturedReceiver = methodReferenceCapturedReceiver;
        this.methodReferenceExpectedArity = methodReferenceExpectedArity;
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
                         if (clazz.isInstance(t)) return InteropLibrary.getUncached().execute(handler, lift(t));
                    } else if (InteropLibrary.getUncached().isMetaInstance(errorType, t)) {
                         return InteropLibrary.getUncached().execute(handler, lift(t));
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
        return JolkNode.lift(value);
    }

    @TruffleBoundary
    private static Object unwrap(Object value) {
        return JolkNode.unwrap(value);
    }

    @TruffleBoundary
    public Object asHostAdapterForMethodReference(Object capturedReceiver, String selector, int expectedArity) {
        // Bound instance: return precise host functional interface for expected arity
        if (capturedReceiver != null) {
            if (expectedArity == 2) {
                return (BinaryOperator<Object>) (a,b) -> {
                    return callWithReceiver(capturedReceiver, selector, new Object[]{a, b});
                };
            } else if (expectedArity == 1) {
                return (Function<Object, Object>) (a) -> {
                    return callWithReceiver(capturedReceiver, selector, new Object[]{a});
                };
            } else if (expectedArity == 0) {
                return (Supplier<Object>) () -> {
                    return callWithReceiver(capturedReceiver, selector, new Object[0]);
                };
            }
        } else {
            // Unbound: host adapter expects receiver as first parameter
            if (expectedArity == 2) {
                return (BinaryOperator<Object>) (rcv,a) -> {
                    return callWithReceiver(rcv, selector, new Object[]{a});
                };
            } else if (expectedArity == 1) {
                return (Function<Object, Object>) (rcv) -> {
                    return callWithReceiver(rcv, selector, new Object[0]);
                };
            }
        }
        return this; // fallback: return the closure itself
    }

    @TruffleBoundary
    private Object callWithReceiver(Object receiver, String selector, Object[] arguments) {
        if (receiver == null) {
            throw new IllegalArgumentException("Receiver is null");
        }
        if (arguments == null) {
            throw new IllegalArgumentException("Arguments is null");
        }
        // Build call args according to closure convention:
        // If the closure captured an environment, the call target expects [env, receiver, ...args]
        // Otherwise the call target expects [receiver, ...args]
        if (environment != null) {
            Object[] callArgs = new Object[arguments.length + 2];
            callArgs[0] = environment;
            callArgs[1] = receiver;
            System.arraycopy(arguments, 0, callArgs, 2, arguments.length);
            return lift(callTarget.call(callArgs));
        } else {
            Object[] callArgs = new Object[arguments.length + 1];
            callArgs[0] = receiver;
            System.arraycopy(arguments, 0, callArgs, 1, arguments.length);
            return lift(callTarget.call(callArgs));
        }
    }

    public boolean isMethodReference() {
        return methodReferenceSelector != null;
    }
    public Object getMethodCapturedReceiver() {
        return methodReferenceCapturedReceiver;
    }
    public String getMethodSelector() {
        return methodReferenceSelector;
    }
    public int getMethodExpectedArity() {
        return methodReferenceExpectedArity;
    }

    public void setMethodReferenceMetadata(Object capturedReceiver, String selector, int expectedArity) {
        this.methodReferenceCapturedReceiver = capturedReceiver;
        this.methodReferenceSelector = selector;
        this.methodReferenceExpectedArity = expectedArity;
    }
}