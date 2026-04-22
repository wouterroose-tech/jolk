package tolk.nodes;

import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.library.CachedLibrary;

import tolk.runtime.JolkClosure;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkObject;
import tolk.runtime.JolkStringExtension;
import tolk.runtime.JolkMatch;
import tolk.runtime.JolkBooleanExtension;
import tolk.runtime.JolkExceptionExtension;
import tolk.runtime.JolkArrayExtension;
import tolk.runtime.JolkLongExtension;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkIntrinsicProtocol;

/// # JolkDispatchNode (The Message Gateway)
///
/// Responsible for executing the **JoMoo Dispatch** logic. It acts as the
/// primary gateway between the Jolk AST and object behavior, ensuring every
/// interaction remains a formal message send.
///
/// This node acts as the terminal enforcer of the **metaboundary**. Because it
/// only resolves messages defined in the **Flattened Registry**, it provides
/// the mechanical foundation for making **intrusive reflection a semantic 
/// impossibility** within the guest language environment.
///
/// This node implements the engine's **Instructional Projection** by mapping 
/// intrinsic selectors directly to optimized machine-code paths during 
/// GraalVM partial evaluation.
///
/// ### Optimization Strategies
///
/// *   **Monomorphic Fast Path**: Specializes for {@link JolkNothing} to 
///     ensure absence checks incur zero overhead.
/// *   **Polymorphic Inline Caches (PICs)**: Utilizes `limit = "3"` to 
///     collapse dispatch for the most frequent receiver types into 
///     high-density machine code via **Semantic Flattening**.
/// *   **Protocol Intrinsification**: Directly handles core control flow 
///     messages (like `#ifPresent` and loops) by executing closures 
///     via optimized call nodes. The current implementation of `doMap`, 
///     `doFilter`, and `doTimes` is specifically laid out to support this 
///     flattening by using `@Shared("callNode") @Cached IndirectCallNode callNode`, 
///     providing the exact "hooks" Graal needs to inline closures and 
///     initiate the Partial Escape Analysis (PEA) that results in Kinetic 
///     **Functional Flow**. Chained Boolean messages (`? :`) are similarly treated 
///     as **Logical Gate Flattening** candidates, allowing the engine to 
///     collapse ternary logic into raw hardware branches.
///
/// Adhering to the principle of **Industrial Sovereignty**, this node 
/// leverages the Truffle DSL to boil away the qualitative overhead of 
/// dynamic messaging.
@GenerateInline(false)
@GenerateCached(true)
public abstract class JolkDispatchNode extends Node {

    /**
     * Executes the message dispatch.
     * 
     * @param frame The current execution frame.
     * @param receiver The object receiving the message.
     * @param selector The message name (selector).
     * @param arguments The arguments passed to the message.
     * @return The result of the message send.
     */
    public abstract Object execute(VirtualFrame frame, Object receiver, String selector, Object[] arguments);

    // --- Helper methods for @Specialization guards ---
    protected static boolean isTimes(String selector) {
        return "times".equals(selector);
    }

    protected static boolean isFilter(String selector) {
        return "filter".equals(selector);
    }

    protected static boolean isForEach(String selector) {
        return "forEach".equals(selector);
    }

    protected static boolean isMap(String selector) {
        return "map".equals(selector);
    }

    protected static boolean isAnyMatch(String selector) {
        return "anyMatch".equals(selector);
    }

    protected static boolean isTernary(String selector) {
        return "? :".equals(selector) || "?! :".equals(selector);
    }

    protected static boolean isTernarySelector(String selector, String expected) {
        return expected.equals(selector);
    }

    protected static boolean isControlFlow(String selector) {
        return switch (selector) {
            case "ifPresent", "ifEmpty", "??", "?", "?!", "? :", "?! :", "finally" -> true;
            default -> false;
        };
    }

    protected static boolean isTry(String selector) {
        return "try".equals(selector);
    }

    protected static boolean isClosureCatch(String selector) {
        return "catch".equals(selector);
    }

    protected static boolean isClosure(Object receiver) {
        return receiver instanceof JolkClosure;
    }

    /**
     * Helper to retrieve an argument safely for DSL guards.
     */
    protected static Object getArg(Object[] arguments, int index) {
        return (arguments != null && index < arguments.length) ? arguments[index] : null;
    }

    /**
     * ### getArg0
     *
     * Helper for Truffle DSL to access arguments in guards/cache expressions.
     * Truffle's expression parser does not support direct array indexing `[]`.
     */
    protected static Object getArg0(Object[] arguments) {
        return arguments.length > 0 ? arguments[0] : null;
    }

    /**
     * ### asClosure
     *
     * Helper for Truffle DSL to cast cached objects to JolkClosure.
     */
    protected static JolkClosure asClosure(Object obj) {
        return obj instanceof JolkClosure ? (JolkClosure) obj : null;
    }

    @Idempotent
    protected static boolean isReifiedMethod(Object member) {
        return member instanceof JolkClosure closure && closure.getEnvironment() == null;
    }

    protected static CallTarget getClosureTarget(Object member) {
        return member instanceof JolkClosure closure ? closure.getCallTarget() : null;
    }

    // --- End of Helper methods ---

    @Specialization(guards = {"isNothing(receiver)", "!isControlFlow(selector)", "!isClosureCatch(selector)"})
    protected Object doNothing(VirtualFrame frame, Object receiver, String selector, Object[] arguments,
                                @Exclusive @CachedLibrary(value = "getNothing()") InteropLibrary interop) {
        // Handle core protocol for Nothing (e.g., #class, #toString) explicitly.
        if (isObjectIntrinsic(selector)) {
            return JolkNode.lift(dispatchObjectIntrinsic(JolkNothing.INSTANCE, selector, arguments, interop));
        }
        try { 
            // ### Safe Navigation (Silent Absorption)
            //
            // Implements the **Safe Navigation** protocol. In Jolk, safe navigation 
            // is an inherent property of the Nothing identity rather than a 
            // syntactic operator. For messages not handled by intrinsics, 
            // Nothing absorbs the message by returning 
            // itself, allowing the communicative flow to continue without 
            // executing logic on an absent state.
            return JolkNode.lift(interop.invokeMember(JolkNothing.INSTANCE, selector, arguments));
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            try {
                // Fallback to host member heuristic for Nothing
                return JolkNode.lift(dispatchHostMember(JolkNothing.INSTANCE, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                // SAFE NAVIGATION: As per the Jolk philosophy, Nothing consumes unknown 
                // messages and returns itself to allow message chains to collapse gracefully.
                return JolkNothing.INSTANCE;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on Nothing", e);
        }
    }

    /// ### Fast Path for Shape-based Property Access (Field Flattening)
    ///
    /// This specialization implements the **protocol-driven flow** for object state. 
    /// By caching the {@link Shape} and the specific {@link Property} offset, 
    /// the engine performs **Instructional Projection**. This collapses a 
    /// dynamic message send (e.g., `#field`) into a direct memory offset load 
    /// or store at the machine level.
    ///
    /// By projecting the object's structural identity into the execution path, 
    /// the engine ensures that the "Lexical Fence" surrounding fields does not 
    /// incur a performance penalty.
    @Specialization(guards = {
        "receiver.getShape() == cachedShape",
        "selector == cachedSelector",
        "property != null"
    }, limit = "3")
    protected Object doShapeRead(VirtualFrame frame, DynamicObject receiver, String selector, Object[] arguments,
                                @Cached("receiver.getShape()") Shape cachedShape,
                                @Cached("selector") String cachedSelector,
                                @Cached("cachedShape.getProperty(selector)") Property property,
                                @CachedLibrary(limit = "3") DynamicObjectLibrary objLib) {

        if (arguments.length == 0) {
            // Getter Pattern: #field
            Object result = objLib.getOrDefault(receiver, cachedSelector, JolkNothing.INSTANCE);
            // Identity Restitution: Ensure null substrate values are lifted to Nothing
            return (result == null) ? JolkNothing.INSTANCE : result;
        } else if (arguments.length == 1) {
            // Setter Pattern: #field(val)
            objLib.put(receiver, cachedSelector, arguments[0]);
            // Self-Return Contract: Setters return the receiver for fluent chaining
            return receiver;
        }

        // Arity mismatch for a field access
        throw new RuntimeException("Invalid arity for field access: " + selector);
    }
    /// ### isNothing
    /// 
    /// Guard used to identify if the receiver should be treated as the Jolk 
    /// Nothing identity.
    /// 
    /// @param receiver The object to check.
    /// @return true if the receiver is null or the JolkNothing instance.
    protected static boolean isNothing(Object receiver) {
        return receiver == null || receiver == JolkNothing.INSTANCE;
    }

    protected JolkNothing getNothing() {
        return JolkNothing.INSTANCE;
    }

    /// ### Fast Path for Closure-based Control Flow
    /// 
    /// Handles #ifPresent and #ifEmpty by directly executing the JolkClosure via 
    /// an IndirectCallNode. This enables Truffle to perform inlining of the 
    /// closure body, significantly improving performance compared to interop execution. 
    /// This method is the primary orchestration site for **Monadic Flow Flattening**; 
    /// by unwrapping `JolkMatch` containers and passing their values to inlined 
    /// closures, the engine enables Graal's PEA to elide the container allocation.
    /// **Logical Gate Flattening** is primarily orchestrated here; the logic 
    /// handles ternary messages (`? :`) by picking a closure and calling it via 
    /// specialized DirectCallNodes to enable full JIT inlining.
    @Specialization(guards = {
        "isControlFlow(selector)",
        "arguments.length == 1",
        "getArg0(arguments) == cachedClosure"
    }, limit = "3")
    protected Object doControlFlowDirect(VirtualFrame frame, Object receiver, String selector, Object[] arguments,
                                        @Cached("getArg0(arguments)") Object cachedClosure,
                                        @Cached("asClosure(cachedClosure)") JolkClosure closure,
                                        @Cached("create(closure.getCallTarget())") DirectCallNode callNode,
                                        @Shared("interop") @CachedLibrary(limit = "3") InteropLibrary interop) {
        Object unwrappedReceiver = JolkNode.unwrap(receiver);
        boolean isAbsent = (unwrappedReceiver == null || unwrappedReceiver == JolkNothing.INSTANCE || interop.isNull(unwrappedReceiver));
        if (unwrappedReceiver instanceof JolkMatch match) isAbsent = !match.isPresent();

        Object env = closure.getEnvironment();
        switch (selector) {
            case "ifPresent" -> {
                if (!isAbsent) {
                    Object val = (receiver instanceof JolkMatch match) ? match.getValue() : receiver;
                    return callNode.call(env, val);
                }
                return JolkNothing.INSTANCE;
            }
            case "ifEmpty", "??" -> {
                if (isAbsent) return callNode.call(env);
                return receiver;
            }
            case "?" -> {
                if (receiver instanceof Boolean b && b) return callNode.call(env);
                return receiver;
            }
            case "?!" -> {
                if (receiver instanceof Boolean b && !b) return callNode.call(env);
                return receiver;
            }
        }
        return doControlFlow(frame, receiver, selector, arguments, IndirectCallNode.getUncached(), interop);
    }

    @Specialization(guards = {
        "isTernary(selector)",
        "arguments.length == 2",
        "getArg0(arguments) == cachedThen",
        "getArg(arguments, 1) == cachedElse"
    }, limit = "3")
    protected Object doTernaryDirect(VirtualFrame frame, Object receiver, String selector, Object[] arguments,
                                    @Cached("getArg0(arguments)") Object cachedThen,
                                    @Cached("getArg(arguments, 1)") Object cachedElse,
                                    @Cached("asClosure(cachedThen)") JolkClosure thenC,
                                    @Cached("asClosure(cachedElse)") JolkClosure elseC,
                                    @Cached("create(thenC.getCallTarget())") DirectCallNode thenNode,
                                    @Cached("create(elseC.getCallTarget())") DirectCallNode elseNode) {
        Object unwrappedReceiver = JolkNode.unwrap(receiver);
        if (unwrappedReceiver instanceof Boolean b) {
            boolean condition = isTernarySelector(selector, "? :") ? b : !b;
            return condition ? thenNode.call(thenC.getEnvironment()) : elseNode.call(elseC.getEnvironment());
        }
        return JolkNothing.INSTANCE;
    }

    @Specialization(guards = "isControlFlow(selector)", replaces = {"doControlFlowDirect", "doTernaryDirect"})
    protected Object doControlFlow(VirtualFrame frame, Object receiver, String selector, Object[] arguments,
                                  @Shared("callNode") @Cached IndirectCallNode callNode,
                                  @Shared("interop") @CachedLibrary(limit = "3") InteropLibrary interop) {
        
        Object unwrappedReceiver = JolkNode.unwrap(receiver);
        // Shared Absence Logic: Handles Nothing, null, and empty Matches 
        boolean isAbsent = (unwrappedReceiver == null || unwrappedReceiver == JolkNothing.INSTANCE || interop.isNull(unwrappedReceiver));
        if (unwrappedReceiver instanceof JolkMatch match) {
            isAbsent = !match.isPresent();
        }

        if (arguments.length == 1 && arguments[0] instanceof JolkClosure closure) {
            switch (selector) {
                case "ifPresent" -> {
                    if (!isAbsent) {
                        Object val = (receiver instanceof JolkMatch match) ? match.getValue() : receiver;
                        return JolkNode.lift(callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment(), JolkNode.lift(val)}));
                    }
                    return JolkNothing.INSTANCE;
                }
                case "ifEmpty" -> {
                    if (isAbsent) {
                        return JolkNode.lift(callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()}));
                    }
                    return receiver;
                }
                case "??" -> {
                    if (isAbsent) {
                        return JolkNode.lift(callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()}));
                    }
                    return receiver;
                }
                case "?" -> {
                    if (receiver instanceof Boolean b && b) {
                        return JolkNode.lift(callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()}));
                    }
                    return receiver;
                }
                case "?!" -> {
                    if (receiver instanceof Boolean b && !b) {
                        return JolkNode.lift(callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()}));
                    }
                    return receiver;
                }
                case "finally" -> {
                    if (unwrappedReceiver instanceof JolkClosure protectedClosure) {
                        // Case 1: Chained directly to a closure [ risky ] #finally [ cleanup ]
                        Object result = JolkNothing.INSTANCE;
                        try {
                            result = callNode.call(protectedClosure.getCallTarget(), new Object[]{protectedClosure.getEnvironment()});
                        } catch (JolkReturnException e) {
                            callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()});
                            throw e;
                        } catch (Throwable t) {
                            callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()});
                            throw t;
                        }
                        callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()});
                        return result;
                    } else {
                        // Case 2: Side-effect on a realized value (e.g., after #catch)
                        callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()});
                        return receiver;
                    }
                }
            }
        }

        // Atomic Ternary with two branches: (condition ? [then] : [else])
        if (arguments.length == 2 && arguments[0] instanceof JolkClosure thenC && arguments[1] instanceof JolkClosure elseC) {
            if ("? :".equals(selector) && unwrappedReceiver instanceof Boolean b) {
                JolkClosure branch = b ? thenC : elseC;
                return JolkNode.lift(callNode.call(branch.getCallTarget(), new Object[]{branch.getEnvironment()}));
            }
            if ("?! :".equals(selector) && unwrappedReceiver instanceof Boolean b) {
                JolkClosure branch = !b ? thenC : elseC;
                return JolkNode.lift(callNode.call(branch.getCallTarget(), new Object[]{branch.getEnvironment()}));
            }
        }
        // Fallback to boundary for non-closure arguments or unhandled control flow
        return JolkNode.lift(dispatchObjectIntrinsic(receiver, selector, arguments, interop));
    }

    /// ### Fast Path for Closure Try-With-Resources
    /// 
    /// Handles `#try` when the receiver is a JolkClosure resource provider. This
    /// specialization executes the resource factory, passes the resulting resource
    /// into the logic closure, and guarantees cleanup via the AutoCloseable bridge.
    @Specialization(guards = {"isTry(selector)", "isClosure(receiver)"})
    protected Object doClosureTry(VirtualFrame frame, Object receiver, String selector, Object[] arguments,
                                  @Shared("callNode") @Cached IndirectCallNode callNode,
                                  @Shared("interop") @CachedLibrary(limit = "3") InteropLibrary interop) {
        if (arguments.length != 1 || !(arguments[0] instanceof JolkClosure logic)) {
            throw new RuntimeException("Invalid #try call: expected a single logic closure argument.");
        }

        JolkClosure provider = (JolkClosure) receiver;
        Object resource = callNode.call(provider.getCallTarget(), new Object[]{provider.getEnvironment()});
        Object liftedResource = JolkNode.lift(resource);

        Throwable thrown = null;
        try {
            return callNode.call(logic.getCallTarget(), new Object[]{logic.getEnvironment(), liftedResource});
        } catch (JolkReturnException e) {
            thrown = e;
            throw e;
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            closeResource(resource, thrown, interop);
        }
    }

    private static void closeResource(Object resource, Throwable prior, InteropLibrary interop) {
        if (resource == null || resource == JolkNothing.INSTANCE) {
            return;
        }

        Object unwrapped = resource;
        // Recursively unwrap host objects if possible
        while (true) {
            Object next = tryUnwrapOnce(unwrapped);
            if (next == unwrapped) break;
            unwrapped = next;
        }

        Throwable closeFailure = null;
        try {
            if (unwrapped instanceof AutoCloseable ac) {
                ac.close();
                return;
            }

            if (interop.isMemberInvocable(resource, "close")) {
                interop.invokeMember(resource, "close");
                return;
            }
            if (interop.isMemberInvocable(unwrapped, "close")) {
                interop.invokeMember(unwrapped, "close");
                return;
            }
        } catch (Throwable closeEx) {
            closeFailure = closeEx;
        }

        if (closeFailure != null) {
            if (prior instanceof Throwable) {
                ((Throwable) prior).addSuppressed(closeFailure);
            } else {
                throw new RuntimeException("Failed to close resource", closeFailure);
            }
        }
    }

    /**
     * Attempts to unwrap a host object one level. If no further unwrapping is possible, returns the same object.
     */
    private static Object tryUnwrapOnce(Object obj) {
        try {
            var env = tolk.language.JolkLanguage.getContext().env;
            if (env != null && env.isHostObject(obj)) {
                return env.asHostObject(obj);
            }
        } catch (Throwable ignored) {}
        // Fallback for Truffle HostObject wrappers
        if (obj != null && obj.getClass().getName().equals("com.oracle.truffle.polyglot.HostObject")) {
            try {
                var method = obj.getClass().getMethod("getJavaObject");
                method.setAccessible(true);
                return method.invoke(obj);
            } catch (Throwable ignored) {}
        }
        return obj;
    }

    /// ### Fast Path for Closure Exception Handling
    /// 
    /// Handles #catch on JolkClosure by routing to the closure's interop protocol.
    /// This enables exception handling for closures.
    @Specialization(guards = {"isClosureCatch(selector)", "isClosure(receiver)"})
    protected Object doClosureCatch(VirtualFrame frame, Object receiver, String selector, Object[] arguments,
                                   @Shared("callNode") @Cached IndirectCallNode callNode,
                                   @Shared("interop") @CachedLibrary(limit = "3") InteropLibrary interop) {
        try {
            if (receiver instanceof JolkClosure riskyClosure && arguments.length == 1 && arguments[0] instanceof JolkClosure handlerClosure) {
                try {
                    // 1. Try to execute the primary closure (the risky logic)
                    return JolkNode.lift(callNode.call(riskyClosure.getCallTarget(), new Object[]{riskyClosure.getEnvironment()}));
                } catch (JolkReturnException e) {
                    throw e; // Non-local returns (^) must propagate past the catch block
                } catch (Throwable e) {
                    // 2. Execute the handler closure with the caught exception
                    return JolkNode.lift(callNode.call(handlerClosure.getCallTarget(), new Object[]{handlerClosure.getEnvironment(), JolkNode.lift(e)}));
                }
            }

            // Fallback: Fix Arity error by prepending receiver (expected 2: receiver + handler)
            Object[] argsWithReceiver = new Object[arguments.length + 1];
            argsWithReceiver[0] = receiver;
            System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
            return JolkNode.lift(interop.invokeMember(receiver, selector, argsWithReceiver));
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        }
    }

    @Specialization(guards = "isClosureCatch(selector)", replaces = "doClosureCatch")
    protected Object doCatchPassThrough(VirtualFrame frame, Object receiver, String selector, Object[] arguments) {
        // If the receiver is not a closure, the 'try' block already succeeded.
        return JolkNode.lift(receiver);
    }

    /// ### Fast Path for Long-based Iteration (#times)
    /// 
    /// As an entry point for **Functional Flow**, this handles the `Long #times [closure]` message by directly executing the JolkClosure
    /// via an IndirectCallNode in a loop. This enables Truffle to perform inlining of the
    /// closure body, significantly improving performance compared to interop execution.
    @Specialization(guards = {"isTimes(selector)", "getArg0(arguments) == cachedClosure"}, limit = "3")
    protected Object doTimesDirect(VirtualFrame frame, Number receiver, String selector, Object[] arguments,
                                  @Cached("getArg0(arguments)") Object cachedClosure,
                                  @Cached("asClosure(cachedClosure)") JolkClosure closure,
                                  @Cached("create(closure.getCallTarget())") DirectCallNode callNode) {
        long limit = receiver.longValue();
        Object env = closure.getEnvironment();
        // INDUSTRIAL OPTIMIZATION: Use call overloads to avoid array traffic entirely in the fast path.
        for (long i = 0; i < limit; i++) {
            callNode.call(env, i);
        }
        return limit;
    }


    @Specialization(guards = "isTimes(selector)", replaces = "doTimesDirect")
    protected Object doTimesIndirect(VirtualFrame frame, Number receiver, String selector, Object[] arguments,
                                    @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure closure) {
            long limit = receiver.longValue();
            Object env = closure.getEnvironment();
            CallTarget target = closure.getCallTarget();
            // Create a reusable argument array to reduce allocation pressure.
            // Note: This is safe here because we are in a sequential loop within one thread.
            Object[] args = new Object[]{env, null};
            for (long i = 0; i < limit; i++) {
                args[1] = i;
                callNode.call(target, args);
            }
            return limit;
        }
        throw new RuntimeException("Invalid arguments for #times: expected a single closure.");
    }

    /// ### Fast Path for Array Filtering (#filter)
    /// 
    /// Implements the filter protocol for java.util.List. The IndirectCallNode 
    /// allows Graal to inline the predicate logic.
    @Specialization(guards = "isFilter(selector)")
    protected Object doFilter(VirtualFrame frame, List<?> receiver, String selector, Object[] arguments,
                              @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure predicate) {
            List<Object> result = new ArrayList<>();
            Object env = predicate.getEnvironment();
            CallTarget target = predicate.getCallTarget();
            Object[] args = new Object[]{env, null};
            for (Object item : receiver) {
                args[1] = item;
                Object matches = callNode.call(target, args);
                if (matches instanceof Boolean b && b) {
                    result.add(item);
                }
            }
            return result;
        }
        throw new RuntimeException("Invalid arguments for #filter: expected a single closure.");
    }

    /// ### Fast Path for Array AnyMatch (#anyMatch)
    /// 
    /// Implements the anyMatch protocol for java.util.List. The IndirectCallNode 
    /// allows Graal to inline the predicate logic. It returns true as soon as 
    /// the predicate returns true for any element, otherwise false.
    @Specialization(guards = "isAnyMatch(selector)")
    protected Object doAnyMatch(VirtualFrame frame, List<?> receiver, String selector, Object[] arguments,
                                @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure predicate) {
            Object env = predicate.getEnvironment();
            CallTarget target = predicate.getCallTarget();
            Object[] args = new Object[]{env, null};
            for (Object item : receiver) {
                args[1] = item;
                Object matches = callNode.call(target, args);
                if (matches instanceof Boolean b && b) {
                    return true;
                }
            }
            return false;
        }
        throw new RuntimeException("Invalid arguments for #anyMatch: expected a single closure.");
    }

    /// ### Fast Path for Iterator Traversal (#forEach)
    /// 
    /// Projects the **Functional Flow** of an Iterator directly into a message-passing 
    /// loop. This is the implementation of the IteratorExtension.
    @Specialization(guards = "isForEach(selector)")
    protected Object doIteratorForEach(VirtualFrame frame, java.util.Iterator<?> receiver, String selector, Object[] arguments,
                                       @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure action) {
            Object env = action.getEnvironment();
            CallTarget target = action.getCallTarget();
            Object[] args = new Object[]{env, null};
            while (receiver.hasNext()) {
                args[1] = receiver.next();
                callNode.call(target, args);
            }
            return JolkNothing.INSTANCE;
        }
        throw new RuntimeException("Invalid arguments for #forEach: expected a single closure.");
    }

    /// ### Fast Path for Map Iteration (#forEach)
    /// 
    /// Maps the associative archetype to a two-argument closure.
    @Specialization(guards = "isForEach(selector)")
    protected Object doMapForEach(VirtualFrame frame, Map<?, ?> receiver, String selector, Object[] arguments,
                                  @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure action) {
            Object env = action.getEnvironment();
            CallTarget target = action.getCallTarget();
            Object[] args = new Object[]{env, null, null};
            for (Map.Entry<?, ?> entry : receiver.entrySet()) {
                args[1] = entry.getKey();
                args[2] = entry.getValue();
                callNode.call(target, args);
            }
            return receiver;
        }
        throw new RuntimeException("Invalid arguments for #forEach: expected a single closure.");
    }

    /// ### Fast Path for Array Projection (#map)
    /// 
    /// Implements the Stream Protocol for java.util.List. While this looks like 
    /// it creates an intermediate list, Graal's Partial Escape Analysis (PEA) 
    /// and Loop Fusion will "boil away" the intermediate allocation when 
    /// messages are chained, resulting in zero-overhead single-pass execution.
    @Specialization(guards = "isMap(selector)")
    protected Object doMap(VirtualFrame frame, List<?> receiver, String selector, Object[] arguments,
                           @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure mapper) {
            List<Object> result = new ArrayList<>(receiver.size());
            Object env = mapper.getEnvironment();
            CallTarget target = mapper.getCallTarget();
            Object[] args = new Object[]{env, null};
            for (Object item : receiver) {
                args[1] = item;
                Object projected = callNode.call(target, args);
                result.add(projected);
            }
            return result;
        }
        throw new RuntimeException("Invalid arguments for #map: expected a single closure.");
    }

    /// ### Fast Path for Longs
    @Specialization(guards = {
        "selector == cachedSelector",
        "isReifiedMethod(cachedMember)"
    }, limit = "3")
    protected Object doLongClosureDirect(VirtualFrame frame, Number receiver, String selector, Object[] arguments,
                                         @Cached("selector") String cachedSelector,
                                         @Cached("lookupLongMember(cachedSelector)") Object cachedMember,
                                         @Cached("getClosureTarget(cachedMember)") CallTarget target,
                                         @Cached("create(target)") DirectCallNode callNode) {
        if (arguments.length == 0) return callNode.call(receiver);
        if (arguments.length == 1) return callNode.call(receiver, arguments[0]);
        Object[] args = new Object[arguments.length + 1];
        args[0] = receiver;
        System.arraycopy(arguments, 0, args, 1, arguments.length);
        return callNode.call((Object)args);
    }

    @Specialization(guards = "selector == cachedSelector", replaces = "doLongClosureDirect", limit = "3")
    protected Object doLongCached(VirtualFrame frame, Number receiver, String selector, Object[] arguments,
                                 @Cached("selector") String cachedSelector,
                                 @Cached("lookupLongMember(cachedSelector)") Object cachedMember,
                                 @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop,
                                 @Shared("fromJavaStringNode") @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
        /**
         * ### Identity Erasure for Primitives
         * 
         * Unlike Java's Type Erasure (which removes generic metadata), Jolk's 
         * **Identity Erasure** physically strips away object structures at the 
         * machine level. This specialization operates directly on the 
         * substrate-native 64-bit scalar to bypass boxing overhead entirely.
         */
        long r = receiver.longValue();

        if (arguments.length == 0 && "toString".equals(cachedSelector)) {
            return fromJavaStringNode.execute(String.valueOf(r), TruffleString.Encoding.UTF_16);
        }

        if (arguments.length == 1 && arguments[0] instanceof Number other) {
            long o = other.longValue();
            switch (cachedSelector) {
                case "*"  -> { return r * o; }
                case "-"  -> { return r - o; }
                case "+"  -> { return r + o; }
                case "/"  -> { return r / o; }
                case "==" -> { return r == o; }
                case "<=" -> { return r <= o; }
                case ">=" -> { return r >= o; }
                case "<"  -> { return r < o; }
                case ">"  -> { return r > o; }
            }
        }

        if (cachedMember != null && !isNothing(cachedMember) && interop.isExecutable(cachedMember)) {
            try {
                InteropLibrary memberInterop = InteropLibrary.getUncached(cachedMember);
                if (arguments.length == 0) return JolkNode.lift(memberInterop.execute(cachedMember, (Object)receiver));
                Object[] args = prepareArguments(cachedMember, receiver, arguments);
                return JolkNode.lift(memberInterop.execute(cachedMember, args));
            } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | RuntimeException e) {
                throw new RuntimeException(e);
            }
        }

        return doLong(frame, receiver, selector, arguments, interop);
    }

    @Specialization(replaces = "doLongCached")
    protected Object doLong(VirtualFrame frame, Number receiver, String selector, Object[] arguments,
                            @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        try {
            Object member = lookupLongMember(selector);
            if (member != null && !isNothing(member) && interop.isExecutable(member)) {
                InteropLibrary memberInterop = InteropLibrary.getUncached(member);
                Object[] args = prepareArguments(member, receiver, arguments);
                return JolkNode.lift(memberInterop.execute(member, args));
            }
            
            if (arguments.length == 1 && "??".equals(selector)) return JolkNode.lift(receiver);
            return JolkNode.lift(interop.invokeMember(receiver, selector, arguments));

        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            if (isObjectIntrinsic(selector)) {
                return JolkNode.lift(dispatchObjectIntrinsic(receiver, selector, arguments, interop));
            }
            try {
                return JolkNode.lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Long", e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on Long", e);
        }
    }

    @TruffleBoundary
    protected Object lookupLongMember(String selector) {
        return JolkLongExtension.LONG_TYPE.lookupInstanceMember(selector);
    }

    /// ### Fast Path for Booleans
    ///
    /// Implements **Identity Erasure** with Inline Caching for Boolean logic.
    @Specialization(guards = "selector == cachedSelector", limit = "3")
    protected Object doBooleanCached(VirtualFrame frame, Boolean receiver, String selector, Object[] arguments,
                                    @Cached("selector") String cachedSelector,
                                    @Cached("lookupBooleanMember(cachedSelector)") Object cachedMember,
                                    @Shared("interop") @CachedLibrary(limit = "3") InteropLibrary interop) {
        if (cachedMember != null && !isNothing(cachedMember) && interop.isExecutable(cachedMember)) {
            try {
                InteropLibrary memberInterop = InteropLibrary.getUncached(cachedMember);
                Object[] args = prepareArguments(cachedMember, receiver, arguments);
                return JolkNode.lift(memberInterop.execute(cachedMember, args));
            } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | RuntimeException e) {
                throw new RuntimeException(e);
            }
        }
        return doBoolean(frame, receiver, selector, arguments, interop);
    }

    @TruffleBoundary
    protected Object lookupBooleanMember(String selector) {
        return JolkBooleanExtension.BOOLEAN_TYPE.lookupInstanceMember(selector);
    }

    @Specialization(replaces = "doBooleanCached")
    protected Object doBoolean(VirtualFrame frame, Boolean receiver, String selector, Object[] arguments,
                                @Shared("interop") @CachedLibrary(limit = "3") InteropLibrary interop) {
        try {
            // 1. Prototype Lookup: Check for Jolk-defined boolean extensions
            Object member = lookupBooleanMember(selector);
            if (member != null && !isNothing(member) && interop.isExecutable(member)) {
                InteropLibrary memberInterop = InteropLibrary.getUncached(member);
                Object[] args = prepareArguments(member, receiver, arguments);
                return JolkNode.lift(memberInterop.execute(member, args));
            }

            // Identity-Based Flow Control: #?? on a non-null Boolean always returns the receiver.
            if (arguments.length == 1 && "??".equals(selector)) return JolkNode.lift(receiver);

            // 2. Host Fallback: Dispatch to standard Java Boolean members (if any)
            return JolkNode.lift(interop.invokeMember(receiver, selector, arguments));

        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            if (JolkIntrinsicProtocol.isObjectIntrinsic(selector)) {
                return JolkNode.lift(dispatchObjectIntrinsic(receiver, selector, arguments, interop));
            }
            try {
                return JolkNode.lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Boolean", e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on Boolean", e);
        }
    }

    /// ### doTruffleString
    /// 
    /// Specialized **Fast Path** for the **String Identity**.
    /// 
    /// This specialization allows [TruffleString] instances to participate 
    /// in the Jolk messaging protocol. Architecturally, this node enforces 
    /// **Identity Congruence**: while the JVM sees a `TruffleObject`, the guest 
    /// environment interacts with a first-class String identity. 
    /// 
    /// This implements **Semantic Flattening** by leveraging `TruffleString` 
    /// for guest-level operations, ensuring memory-efficient deduplication 
    /// and high-performance instructional projection during the JIT phase.
    @Specialization(guards = "receiver != null")
    protected Object doTruffleString(VirtualFrame frame, TruffleString receiver, String selector, Object[] arguments,
                             @Cached TruffleString.ToJavaStringNode toJavaStringNode,
                             @Cached TruffleString.CodePointLengthNode lengthNode,
                             @Cached TruffleString.EqualNode equalNode,
                             @Shared("interop") @CachedLibrary(limit = "3") InteropLibrary interop,
                             @Cached TruffleString.ConcatNode concatNode,
                             @Cached TruffleString.FromLongNode fromLongNode) {
       
        // The internal encoding used for all Jolk String Identities
        TruffleString.Encoding encoding = TruffleString.Encoding.UTF_16;

        try {
            // FAST PATH: Intrinsify metadata and basic operations to avoid materialization.
            if (arguments.length == 0) {
                if ("length".equals(selector)) return (long) lengthNode.execute(receiver, encoding);
                if ("isEmpty".equals(selector)) return lengthNode.execute(receiver, encoding) == 0;
            }
            
            if (arguments.length == 1 && "+".equals(selector)) {
                Object other = arguments[0];
                if (other instanceof TruffleString ts) {
                    return concatNode.execute(receiver, ts, encoding, true);
                } else if (other instanceof Long l) {
                    return concatNode.execute(receiver, fromLongNode.execute(l, encoding, true), encoding, true);
                }
                // Fallback for other types via interop
            }

            if (arguments.length == 1 && "??".equals(selector)) return JolkNode.lift(receiver);

            // 0. Intrinsic Lookup: Handle core protocol (like #class or #hash) before materialization.
            if (isObjectIntrinsic(selector)) {
                return JolkNode.lift(dispatchObjectIntrinsic(receiver, selector, arguments, interop));
            }

            if (arguments.length == 0) {
                // For complex transformations, we lower to Java String temporarily 
                // until Jolk-native TruffleString extensions are fully implemented.
                if ("toUpperCase".equals(selector)) return JolkNode.lift(toJavaStringNode.execute(receiver).toUpperCase());
                if ("toLowerCase".equals(selector)) return JolkNode.lift(toJavaStringNode.execute(receiver).toLowerCase());
                if ("trim".equals(selector)) return JolkNode.lift(toJavaStringNode.execute(receiver).trim());
            }
            
            if (arguments.length == 1 && "+".equals(selector)) {
                return JolkNode.lift(toJavaStringNode.execute(receiver) + String.valueOf(arguments[0]));
            }

            if (arguments.length == 1 && "matches".equals(selector)) {
                return JolkNode.lift(toJavaStringNode.execute(receiver).matches(interop.asString(arguments[0])));
            }

            // 1. Prototype Lookup: Check for Jolk-defined string extensions
            Object member = lookupStringMember(selector);
            if (member != null && !isNothing(member) && interop.isExecutable(member)) {
                Object[] args = prepareArguments(member, receiver, arguments);
                return JolkNode.lift(interop.execute(member, args));
            }

            // 2. Host Fallback: Lowering to java.lang.String for interoperability
            return JolkNode.lift(interop.invokeMember(toJavaStringNode.execute(receiver), selector, arguments));
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            try {
                return JolkNode.lift(dispatchHostMember(toJavaStringNode.execute(receiver), selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on TruffleString", e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on TruffleString", e);
        }
    }
    

    @TruffleBoundary
    protected Object lookupStringMember(String selector) {
        return JolkStringExtension.STRING_TYPE.lookupInstanceMember(selector);
    }

    /// ### doJavaString (Identity Restitution)
    ///
    /// Handles raw [java.lang.String] instances by "lifting" them into the 
    /// [TruffleString] identity. This implements the **Identity Restitution** 
    /// protocol at the metaboundary.
    @Specialization
    protected Object doJavaString(VirtualFrame frame, String receiver, String selector, Object[] arguments,
                                 @Shared("fromJavaStringNode") @Cached TruffleString.FromJavaStringNode fromJavaStringNode,
                                 @Cached JolkDispatchNode dispatchNode) {
        // Lift the host string into the guest identity
        TruffleString lifted = fromJavaStringNode.execute(receiver, TruffleString.Encoding.UTF_16);
        return dispatchNode.execute(frame, lifted, selector, arguments);
    }

    /// ### Fast Path for Lists
    /// Handles java.util.List instances by routing messages to the Jolk Array extension.
    @Specialization
    protected Object doList(VirtualFrame frame, List<?> receiver, String selector, Object[] arguments,
                           @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        try {
            // 1. Prototype Lookup: Check for Jolk-defined Array extensions (e.g., #at, #put)
            Object member = lookupArrayMember(selector);
            if (member != null && !isNothing(member) && interop.isExecutable(member)) {
                Object[] args = prepareArguments(member, receiver, arguments);
                return JolkNode.lift(interop.execute(member, args));
            }

            // 2. Host Fallback: Dispatch to standard java.util.List members
            return JolkNode.lift(interop.invokeMember(receiver, selector, arguments));
        } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            if (JolkIntrinsicProtocol.isObjectIntrinsic(selector)) {
                return JolkNode.lift(dispatchObjectIntrinsic(receiver, selector, arguments, interop));
            }
            try {
                return JolkNode.lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on List", e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on List", e);
        }
    }

    @TruffleBoundary
    protected Object lookupArrayMember(String selector) {
        return JolkArrayExtension.ARRAY_TYPE.lookupInstanceMember(selector);
    }

    /// ### doThrowable
    ///
    /// Specialized **Intrinsic Fast Path** for host exceptions.
    /// 
    /// This specialization ensures that [java.lang.Throwable] instances prioritize 
    /// Jolk's internal control-flow logic (like `#throw`) over standard Java 
    /// member lookup. This is critical for maintaining **Shim-less Integration** 
    /// where the language must control the stack unwinding process.
    @Specialization
    protected Object doThrowable(VirtualFrame frame, Throwable receiver, String selector, Object[] arguments, 
                               @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        if ("throw".equals(selector)) {
            if (arguments.length != 0) {
                throw new RuntimeException("The instance #throw selector does not accept arguments. Use the meta Exception #throw(message) instead.");
            }
            JolkExceptionExtension.throwException(receiver);
            return JolkNothing.INSTANCE;
        }
        try {
            Object result = interop.invokeMember(receiver, selector, arguments);
            return JolkNode.lift(result);
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            if (JolkIntrinsicProtocol.isObjectIntrinsic(selector)) {
                return JolkNode.lift(dispatchObjectIntrinsic(receiver, selector, arguments, interop));
            }
            try {
                return JolkNode.lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Throwable", e);
            }
        }
    }

    /// ### Fast Path for User-Defined Methods
    ///
    /// Specializes the dispatch for Jolk objects (DynamicObject). By caching the 
    /// method lookup result based on the shape and selector, we eliminate the 
    /// @TruffleBoundary overhead that normally kills recursive performance.
    /// ### Fast Path for User-Defined Methods (Recursive Optimization)
    ///
    /// By specializing for `JolkClosure` and using a `DirectCallNode`, we allow
    /// the Graal JIT to inline recursive calls (like Fibonacci). This collapses
    /// the call boundary and allows Partial Escape Analysis to elide argument arrays.
    @Specialization(guards = {
        "receiver.getShape() == cachedShape",
        "selector == cachedSelector",
        "isReifiedMethod(member)"
    }, limit = "3")
    protected Object doUserDirect(VirtualFrame frame, DynamicObject receiver, String selector, Object[] arguments,
                                   @Cached("receiver.getShape()") Shape cachedShape,
                                   @Cached("selector") String cachedSelector,
                                   @Cached("asClosure(lookupUserMember(receiver, cachedSelector))") JolkClosure member,
                                   @Cached("create(member.getCallTarget())") DirectCallNode callNode) {
        // Optimized recursion: handle common arities without intermediate array allocation
        int arity = arguments.length;
        if (arity == 1) {
            // Pattern: factorial(n) -> [Self, n]
            return JolkNode.lift(callNode.call(receiver, arguments[0]));
        } else if (arity == 0) {
            return JolkNode.lift(callNode.call(receiver));
        } else {
            // Generic case: Jolk Calling Convention [Self, ...Args]
            Object[] callArgs = new Object[arity + 1];
            callArgs[0] = receiver;
            System.arraycopy(arguments, 0, callArgs, 1, arity);
            return JolkNode.lift(callNode.call(callArgs));
        }
    }

    @Specialization(guards = {
        "receiver.getShape() == cachedShape",
        "selector == cachedSelector"
    }, limit = "3", replaces = "doUserDirect")
    protected Object doUserDispatch(VirtualFrame frame, DynamicObject receiver, String selector, Object[] arguments,
                                   @Cached("receiver.getShape()") Shape cachedShape,
                                   @Cached("selector") String cachedSelector,
                                   @Cached("lookupUserMember(receiver, cachedSelector)") Object member,
                                   @Shared("interop") @CachedLibrary(limit = "3") InteropLibrary interop) {
        if (member != null && !isNothing(member) && interop.isExecutable(member)) {
            try {
                InteropLibrary memberInterop = InteropLibrary.getUncached(member); // Get interop for the member
                Object[] args = prepareArguments(member, receiver, arguments);
                return JolkNode.lift(memberInterop.execute(member, args));
            } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | RuntimeException e) {
                throw new RuntimeException(e);
            }
        }
        // If member is null, fall back to generic dispatch (e.g. host members)
        return doDispatch(frame, receiver, selector, arguments, interop);
    }

    /// ### Generic Dispatch
    ///
    /// This is the fallback for any object that is not `JolkNothing`. It uses a polymorphic
    /// inline cache (`limit = "3"`) to handle different receiver types efficiently.
    @Specialization(replaces = {
        "doNothing", "doShapeRead", "doUserDispatch", "doClosureTry", 
        "doClosureCatch", "doCatchPassThrough", 
        "doControlFlowDirect", "doTernaryDirect", "doControlFlow",
        "doTimesDirect", "doTimesIndirect", "doMap", "doFilter", "doAnyMatch", 
        "doIteratorForEach", "doMapForEach", "doLongClosureDirect", "doLongCached", 
        "doLong", "doBooleanCached", "doBoolean", "doTruffleString", "doJavaString", "doList", "doThrowable"
    })
    protected Object doDispatch(VirtualFrame frame, Object receiver, String selector, Object[] arguments,
                                @Shared("interop") @CachedLibrary(limit = "3") InteropLibrary interop) {
        Object unwrappedReceiver = JolkNode.unwrap(receiver);
        try {
            // 1. Receiver Restitution: Handle raw Java null or Interop null as Jolk Nothing identity.
            // We explicitly check for JolkNothing.INSTANCE to support the identity even 
            // if it no longer exports isNull() to the substrate.
            if (isNothing(unwrappedReceiver) || interop.isNull(receiver)) {
                if (isObjectIntrinsic(selector)) {
                    return JolkNode.lift(dispatchObjectIntrinsic(JolkNothing.INSTANCE, selector, arguments, interop));
                }
                
                // Re-use logic for Nothing lookup to ensure Graal can inline it.
                Object member = lookupNothingMember(selector);
                if (member != null && !isNothing(member) && interop.isExecutable(member)) {
                    Object[] args = prepareArguments(member, JolkNothing.INSTANCE, arguments);
                    return JolkNode.lift(interop.execute(member, args));
                }

                try {
                    // Identity Restitution: Fallback to host member heuristic for Nothing
                    return JolkNode.lift(dispatchHostMember(JolkNothing.INSTANCE, selector, arguments));
                } catch (UnknownIdentifierException ex) {
                    // SAFE NAVIGATION: Gracefully collapse the message chain.
                    return JolkNothing.INSTANCE;
                }
            }

            // 2. Jolk Prototype Lookup (Megamorphic / Generic Path)
            // We normalize the receiver lookup by checking the unwrapped identity.
            JolkMetaClass meta = null;
            if (unwrappedReceiver instanceof Number) meta = JolkLongExtension.LONG_TYPE;
            else if (unwrappedReceiver instanceof Boolean) meta = JolkBooleanExtension.BOOLEAN_TYPE;
            else if (unwrappedReceiver instanceof String || unwrappedReceiver instanceof TruffleString) meta = JolkStringExtension.STRING_TYPE;
            else if (unwrappedReceiver instanceof List) meta = JolkArrayExtension.ARRAY_TYPE;
            else if (unwrappedReceiver instanceof Throwable) meta = JolkExceptionExtension.EXCEPTION_TYPE;

            // 3. User-Defined Object Lookup (#factorial path)
            // We must check the unwrapped receiver to handle Jolk instances that arrive 
            // via the Polyglot boundary (which are wrapped in HostObjects).
            if (meta == null && unwrappedReceiver instanceof DynamicObject dobj) {
                if (dobj.getShape().getDynamicType() instanceof JolkMetaClass jmc) meta = jmc;
            }

            if (meta != null) {
                Object member = lookupMetaInstanceMember(meta, selector);
                if (member != null && !isNothing(member) && interop.isExecutable(member)) {
                    InteropLibrary memberInterop = InteropLibrary.getUncached(member); // Get interop for the member
                    Object[] args = prepareArguments(member, receiver, arguments);
                    return JolkNode.lift(memberInterop.execute(member, args));
                }
            }

            // 3b. Meta-Method Lookup: If the receiver is a Jolk MetaClass, check its custom meta-registry.
            if (meta == null && unwrappedReceiver instanceof tolk.runtime.JolkMetaClass jmc) {
                Object member = lookupMetaMember(jmc, selector);
                if (member != null && !isNothing(member) && interop.isExecutable(member)) {
                    Object[] args = prepareArguments(member, receiver, arguments);
                    return JolkNode.lift(interop.execute(member, args));
                }
            }

            /// ### Meta-Object Interceptor (#new)
            /// 
            /// Implements the **Unified Messaging** rule for object creation. If the receiver 
            /// is a host [Class] (MetaObject) and the selector is `#new`, we map it 
            /// directly to the Interop `instantiate` protocol.
            if ("new".equals(selector) && (receiver instanceof Class || interop.isMetaObject(receiver) || interop.isInstantiable(receiver))) {
                // Shim-less Interceptor: Route List.class, ArrayList.class, or the Jolk Array MetaClass 
                // to the specialized Array factory logic.
                if (receiver == List.class || receiver == ArrayList.class || receiver == JolkArrayExtension.ARRAY_TYPE) {
                    try {
                        return JolkArrayExtension.ARRAY_TYPE.callMetaMember("new", arguments);
                    } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                        throw new RuntimeException("Failed to instantiate Jolk Array from host class: " + receiver, e);
                    }
                }
                
                try {
                    // Truffle DSL can sometimes generate ambiguous specializations if two methods
                    // have the same guard and receiver type. Adding explicit 'replaces'
                    // annotations helps the DSL resolve this ambiguity, ensuring that
                    // only one specialization is chosen for a given receiver/selector combination.
                    if (interop.isInstantiable(receiver)) {
                        return JolkNode.lift(interop.instantiate(receiver, arguments));
                    }
                    return JolkNode.lift(dispatchHostMember(receiver, selector, arguments));
                } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException | RuntimeException e) {
                    throw new RuntimeException("Failed to instantiate host object: " + receiver + " with arguments.", e);
                }
            }

            try {
                return JolkNode.lift(interop.invokeMember((Object) receiver, selector, arguments));
            } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                // Identity Restitution Protocol: Intrinsic messages act as a fallback 
                // for all objects that do not explicitly override them.
                if (isObjectIntrinsic(selector)) {
                    return JolkNode.lift(dispatchObjectIntrinsic(receiver, selector, arguments, interop));
                }
                // Impedance Resolution: Fallback to host member heuristic
                try {
                    return JolkNode.lift(dispatchHostMember(receiver, selector, arguments));
                } catch (UnknownIdentifierException ex) {
                    throw new RuntimeException("Message dispatch failed: #" + selector, e);
                }
            }
        } catch (JolkReturnException e) {
            throw e;
        } catch (Exception e) {
            // Use the full class name to distinguish between Value wrappers and internal HostObjects.
            String receiverStr = (receiver == null) ? "null" : receiver.getClass().getName() + " [" + receiver + "]";
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiverStr, e);
        }
    }

    /**
     * ### prepareArguments
     * 
     * Ensures arguments match the Jolk calling convention for the target member.
     */
    protected Object[] prepareArguments(Object member, Object receiver, Object[] arguments) {
        if (member instanceof JolkClosure closure) {
            if (closure.getEnvironment() == null) {
                // Reified Method: Prepend only receiver (Self at 0)
                Object[] args = new Object[arguments.length + 1];
                args[0] = receiver;
                System.arraycopy(arguments, 0, args, 1, arguments.length);
                return args;
            } else {
                // Reified Closure: Prepend environment (Env at 0)
                Object[] args = new Object[arguments.length + 1];
                args[0] = closure.getEnvironment();
                System.arraycopy(arguments, 0, args, 1, arguments.length);
                return args;
            }
        } else {
            // Built-in or Host Member: Prepend receiver (Self at 0)
            Object[] args = new Object[arguments.length + 1];
            args[0] = receiver;
            System.arraycopy(arguments, 0, args, 1, arguments.length);
            return args;
        }
    }

    @TruffleBoundary
    protected Object lookupUserMember(DynamicObject receiver, String selector) {
        if (receiver.getShape().getDynamicType() instanceof JolkMetaClass meta) {
            return meta.lookupInstanceMember(selector);
        }
        return null;
    }

    @TruffleBoundary
    protected Object lookupNothingMember(String selector) {
        return JolkNothing.NOTHING_TYPE.lookupInstanceMember(selector);
    }

    @TruffleBoundary
    protected Object lookupMetaInstanceMember(JolkMetaClass meta, String selector) {
        return meta.lookupInstanceMember(selector);
    }

    @TruffleBoundary
    protected Object lookupMetaMember(JolkMetaClass meta, String selector) {
        return meta.lookupMetaMember(selector);
    }

    /// ### isObjectIntrinsic
    ///
    /// Checks if a member name belongs to the Jolk Core Protocol. This method 
    /// serves as the runtime implementation of the 
    /// `extension ObjectExtension on java.lang.Object` declaration.
    ///
    /// @param member The selector name to check.
    /// @return true if the selector is a Jolk intrinsic.
    public static boolean isObjectIntrinsic(String member) {
        if (member == null) return false;
        return switch (member) {
            case "new", "catch", "finally", "throw", "==", "!=", "~~", "!~", "??", "hash", "toString", "class", 
                 "instanceOf", "isPresent", "isEmpty", "ifPresent", "ifEmpty", 
                 "?", "? :", "?!", "?! :" -> true;
            default -> false;
        };
    }

    @TruffleBoundary
    public static Object dispatchObjectIntrinsic(Object receiver, String name, Object[] arguments, InteropLibrary interop) {
        InteropLibrary genericInterop = InteropLibrary.getUncached();
        try {
            switch (name) {
                case "==" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    // Identity short-circuit: same instance is always identical.
                    if (receiver == other) return true;

                    // Identity Congruence: intrinsic types match by value to ensure identity 
                    // is tied to value regardless of boxed storage.
                    if (receiver instanceof Number n1 && other instanceof Number n2) {
                        return n1.longValue() == n2.longValue();
                    }
                    if (receiver instanceof Boolean b1 && other instanceof Boolean b2) {
                        return b1.booleanValue() == b2.booleanValue();
                    }
                    
                    if (interop.isString(receiver) && genericInterop.isString(other)) {
                        try {
                            return interop.asString(receiver).equals(genericInterop.asString(other));
                        } catch (UnsupportedMessageException e) {
                        }
                    }

                    // Only use InteropLibrary.isIdentical if both are interoperable types.
                    // Otherwise, for distinct plain Java Objects, they are not identical.
                    if (receiver instanceof TruffleObject || other instanceof TruffleObject ||
                        receiver instanceof Boolean || other instanceof Boolean ||
                        receiver instanceof Byte || other instanceof Byte ||
                        receiver instanceof Short || other instanceof Short ||
                        receiver instanceof Integer || other instanceof Integer ||
                        receiver instanceof Long || other instanceof Long ||
                        receiver instanceof Float || other instanceof Float ||
                        receiver instanceof Double || other instanceof Double ||
                        receiver instanceof Character || other instanceof Character ||
                        receiver instanceof String || other instanceof String) {
                        return interop.isIdentical(receiver, other, genericInterop);
                    }
                    return false; // Distinct plain Java objects are not identical
                }
                case "!=" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    if (receiver == other) return false;

                    // Identity Congruence: intrinsic types match by value
                    if (receiver instanceof Number n1 && other instanceof Number n2) {
                        return n1.longValue() != n2.longValue();
                    }
                    if (receiver instanceof Boolean b1 && other instanceof Boolean b2) {
                        return b1.booleanValue() != b2.booleanValue();
                    }
                    
                    if (interop.isString(receiver) && genericInterop.isString(other)) {
                        try {
                            return !interop.asString(receiver).equals(genericInterop.asString(other));
                        } catch (UnsupportedMessageException e) {
                        }
                    }

                    // Only use InteropLibrary.isIdentical if both are interoperable types.
                    // Otherwise, for distinct plain Java Objects, they are not identical.
                    if (receiver instanceof TruffleObject || other instanceof TruffleObject ||
                        receiver instanceof Boolean || other instanceof Boolean ||
                        receiver instanceof Byte || other instanceof Byte ||
                        receiver instanceof Short || other instanceof Short ||
                        receiver instanceof Integer || other instanceof Integer ||
                        receiver instanceof Long || other instanceof Long ||
                        receiver instanceof Float || other instanceof Float ||
                        receiver instanceof Double || other instanceof Double ||
                        receiver instanceof Character || other instanceof Character ||
                        receiver instanceof String || other instanceof String) {
                        return !interop.isIdentical(receiver, other, genericInterop);
                    }
                    return true; // Distinct plain Java objects are non-identical
                }
                case "~~" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    // Equivalence defaults to Identity behavior for primitives and falls back to equals()
                    if (receiver instanceof Number n1 && other instanceof Number n2) {
                        return n1.longValue() == n2.longValue();
                    }
                    
                    if (interop.isString(receiver) && genericInterop.isString(other)) {
                        try {
                            return interop.asString(receiver).equals(genericInterop.asString(other));
                        } catch (UnsupportedMessageException e) {
                        }
                    }
                    
                    return receiver.equals(other);
                }
                case "!~" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    if (receiver instanceof Number n1 && other instanceof Number n2) {
                        return n1.longValue() != n2.longValue();
                    }
                    if (interop.isString(receiver) && genericInterop.isString(other)) {
                        try {
                            return !interop.asString(receiver).equals(genericInterop.asString(other));
                        } catch (UnsupportedMessageException e) {
                        }
                    }
                    return !receiver.equals(other);
                }
                case "??" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    // Identity-Based Flow Control: execute fallback only if receiver is Nothing.
                    if (receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver)) {
                        Object arg = arguments[0];
                        // Support both direct values (null ?? 42) and closures (null ?? [ ^42 ])
                        Object result = genericInterop.isExecutable(arg) ? genericInterop.execute(arg) : arg;
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver;
                }
                case "hash" -> { 
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (isNothing(receiver)) return 0L;
                    return (long) receiver.hashCode();
                }
                case "toString" -> { 
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (isNothing(receiver)) return "null";
                    return JolkNode.lift(receiver.toString());
                }
                case "isPresent" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof JolkMatch match) return match.isPresent();
                    return receiver != null && receiver != JolkNothing.INSTANCE && !interop.isNull(receiver);
                }
                case "isEmpty" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof JolkMatch match) return !match.isPresent();
                    return receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver);
                }
                case "ifPresent" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver)) return JolkNothing.INSTANCE;
                    if (receiver instanceof JolkMatch match) {
                        if (match.isPresent()) {
                            Object val = match.getValue();
                            Object result = genericInterop.execute(arguments[0], (val == null || genericInterop.isNull(val)) ? JolkNothing.INSTANCE : val);
                            return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                        }
                        return JolkNothing.INSTANCE;
                    }
                    Object result = genericInterop.execute(arguments[0], receiver);
                    return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                }
                case "ifEmpty" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver)) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    if (receiver instanceof JolkMatch match) {
                        if (!match.isPresent()) {
                            Object result = genericInterop.execute(arguments[0]);
                            return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                        }
                        return receiver;
                    }
                    return receiver;
                }
                case "class" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof Long || receiver instanceof Integer) return JolkLongExtension.LONG_TYPE;
                    if (receiver instanceof Boolean) return JolkBooleanExtension.BOOLEAN_TYPE;
                    if (receiver instanceof JolkMetaClass) return receiver;
                    // Identity Restitution: Use the interop protocol to resolve specific host classes
                    // (e.g. java.lang.RuntimeException) via our extensions or host defaults.
                    return interop.hasMetaObject(receiver) ? interop.getMetaObject(receiver) : JolkNothing.NOTHING_TYPE;
                }
                case "instanceOf" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object type = arguments[0];
                    return (genericInterop.isMetaObject(type) && genericInterop.isMetaInstance(type, receiver)) ? JolkMatch.with(receiver) : JolkMatch.empty();
                }
                case "?" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver instanceof Boolean b && b) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver; // Binary branching returns receiver
                }
                case "?!" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver instanceof Boolean b && !b) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver;
                }
                case "? :" -> {
                    if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                    if (receiver instanceof Boolean b) {
                        Object result = b ? genericInterop.execute(arguments[0]) : genericInterop.execute(arguments[1]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return JolkNothing.INSTANCE;
                }
                case "?! :" -> {
                    if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                    if (receiver instanceof Boolean b) {
                        Object result = !b ? genericInterop.execute(arguments[0]) : genericInterop.execute(arguments[1]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return JolkNothing.INSTANCE;
                }
                case "new" -> {
                    // Jolk Identity Resolution: If the receiver is a Jolk MetaClass, 
                    // we perform the intrinsic instantiation (allocation). We use the 
                    // JolkObject constructor directly to bypass Jolk-level meta-methods, 
                    // ensuring we avoid infinite recursion loops during super-dispatch.
                    if (receiver instanceof JolkMetaClass meta) {
                        try {
                            return JolkNode.lift(new JolkObject(meta, arguments));
                        } catch (Exception ignored) {}
                    }

                    // Shim-less Interceptor: Route List.class or ArrayList.class to the specialized Array factory logic.
                    if (receiver == List.class || receiver == java.util.ArrayList.class) {
                        try {
                            return JolkArrayExtension.ARRAY_TYPE.callMetaMember("new", arguments);
                        } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException ignored) {}
                    }
                    // Unified Instantiation: If the receiver is instantiable (like a JolkMetaClass), 
                    // we perform the creation protocol directly.
                    if (interop.isInstantiable(receiver)) {
                        try {
                            return JolkNode.lift(interop.instantiate(receiver, arguments));
                        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException ignored) {}
                    }
                    // Identity Restitution: Final attempt to instantiate via reflection if interop failed
                    Object unwrapped = JolkNode.unwrap(receiver);
                    if (unwrapped instanceof Class<?> clazz) {
                        return tryInvokeViaReflection(clazz, "new", arguments);
                    }
                    return JolkNothing.INSTANCE;
                }
                case "throw" -> {
                    JolkExceptionExtension.throwException(receiver instanceof Throwable t ? t : new RuntimeException(String.valueOf(receiver)));
                    return JolkNothing.INSTANCE;
                }
            }
        } catch (JolkReturnException e) {
            throw e;
        } catch (Throwable e) {
            if (e instanceof JolkReturnException re) throw re;
            throw new RuntimeException("Intrinsic dispatch failed: #" + name, e);
        }
        return null;
    }

    /// ### dispatchHostMember
    /// 
    /// Implements the **Shim-less Integration** heuristic by attempting to map a Jolk 
    /// selector to a native Java member on a host object. It attempts the exact name, 
    /// followed by common Java Bean patterns (get/is/set) and public field access.
    /// 
    /// @param receiver The host object receiving the message.
    /// @param selector The Jolk selector (e.g., "name").
    /// @param arguments The call arguments.
    /// @return The result of the invocation or the receiver in case of a setter.
    /// @throws UnknownIdentifierException If no matching host member is found.
    @TruffleBoundary
    private static Object dispatchHostMember(Object receiver, String selector, Object[] arguments) throws UnknownIdentifierException {
        InteropLibrary interop = InteropLibrary.getUncached();
        String capitalized = capitalize(selector);

        // 1. Try Method/Getter/Field candidates (exact, get, is)
        String[] candidates = {selector, "get" + capitalized, "is" + capitalized};
        for (String candidate : candidates) {
            try {
                if (interop.isMemberInvocable(receiver, candidate)) {
                    // Try to convert arguments using reflection to get parameter types
                    Object[] convertedArgs = convertArgumentsForMethod(receiver, candidate, arguments);
                    Object[] finalArgs = convertedArgs != null ? convertedArgs : arguments;
                    if (convertedArgs != null) {
                        // Identity Restitution: Coerced host objects (like Lambdas) must be lifted into TruffleObjects
                        for (int i = 0; i < finalArgs.length; i++) {
                            finalArgs[i] = JolkNode.lift(finalArgs[i]);
                        }
                    }
                    return interop.invokeMember(receiver, candidate, finalArgs);
                }
            } catch (UnknownIdentifierException e) {
                // continue
            } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                throw new RuntimeException(e);
            }
            
            // Impedance Resolution: Fallback to Java Reflection for types that 
            // Interop treats as values without members (String, Long, Boolean).
            Object reflected = tryInvokeViaReflection(receiver, candidate, arguments);
            if (reflected != null) return reflected;

            // Support for public fields (Impedance Resolution)
            if (arguments.length == 0 && interop.isMemberReadable(receiver, candidate)) {
                try {
                    return interop.readMember(receiver, candidate);
                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                    // continue
                }
            }
        }

        // 2. Try Setter candidate (Fluent Pattern: #name(val) -> returns receiver)
        if (arguments.length == 1) {
            String setter = "set" + capitalized;
            if (interop.isMemberInvocable(receiver, setter)) {
                try {
                    interop.invokeMember(receiver, setter, arguments);
                    return receiver; 
                } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        throw UnknownIdentifierException.create(selector);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @TruffleBoundary
    private static Object tryInvokeViaReflection(Object receiver, String methodName, Object[] arguments) {
        InteropLibrary interop = InteropLibrary.getUncached();
        
        // Impedance Resolution: Prepare unboxed arguments for Java Reflection.
        // Jolk identities (JolkLong, JolkBoolean) and polyglot types must be 
        // converted to standard Java types to match reflection signatures.
        Object[] unboxed = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            Object arg = arguments[i];
            if (arg == null || arg == JolkNothing.INSTANCE) unboxed[i] = null;
            else if (arg instanceof JolkLongExtension jl) unboxed[i] = JolkLongExtension.asLong(jl);
            else if (arg instanceof JolkBooleanExtension jb) unboxed[i] = JolkBooleanExtension.asBoolean(jb);
            else if (interop.isString(arg)) {
                try { unboxed[i] = interop.asString(arg); } catch (UnsupportedMessageException e) { unboxed[i] = arg; }
            } else if (interop.isNumber(arg)) {
                try { unboxed[i] = interop.asLong(arg); } catch (UnsupportedMessageException e) { unboxed[i] = arg; }
            } else {
                unboxed[i] = arg;
            }
        }

        Object unwrapped = JolkNode.unwrap(receiver);
        try {
            // 1. Meta-Object Instantiation: If selector is #new and receiver is a Class, 
            // map directly to constructors to support Jolk's object creation protocol.
            if ("new".equals(methodName) && unwrapped instanceof Class<?> clazz) {
                for (java.lang.reflect.Constructor<?> c : clazz.getConstructors()) {
                    Object[] matched = matchArguments(c.getParameterTypes(), c.isVarArgs(), unboxed);
                    if (matched != null) {
                        Object[] coerced = coerceArguments(c.getParameterTypes(), matched);
                        try {
                            return JolkNode.lift(c.newInstance(coerced));
                        } catch (Exception e) {
                            // continue to next candidate
                        }
                    }
                }
            }

            // 2. Member Invocation: Simple heuristic for Shim-less Integration.
            // Allows Jolk to reach native methods on types like String and Long 
            // which are often treated as "memberless" values by standard Interop.
            if (unwrapped == null) return null;
            Class<?> lookupClass = (unwrapped instanceof Class<?> clazz) ? clazz : unwrapped.getClass();
            for (java.lang.reflect.Method m : lookupClass.getMethods()) {
                if (m.getName().equals(methodName)) {
                    Object[] matched = matchArguments(m.getParameterTypes(), m.isVarArgs(), unboxed);
                    if (matched != null) {
                        Object[] coerced = coerceArguments(m.getParameterTypes(), matched);
                        try {
                            Object result = m.invoke(java.lang.reflect.Modifier.isStatic(m.getModifiers()) ? null : receiver, coerced);
                            if (m.getReturnType() == void.class) {
                                // Receiver Retention: return receiver for void methods
                                return receiver;
                            }
                            return JolkNode.lift(result);
                        } catch (Exception e) {
                            // continue to next candidate
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            // Reflection may be restricted in some environments; fail silently to allow other dispatch paths.
        }
        return null;
    }

    /// ### matchArguments
    /// 
    /// Matches provided arguments against a parameter signature, handling 
    /// Java varargs by wrapping trailing arguments into an array.
    private static Object[] matchArguments(Class<?>[] types, boolean isVarArgs, Object[] args) {
        if (!isVarArgs) {
            return (types.length == args.length) ? args : null;
        }
        
        int fixedCount = types.length - 1;
        if (args.length < fixedCount) return null;

        Object[] result = new Object[types.length];
        System.arraycopy(args, 0, result, 0, fixedCount);

        // Wrap trailing arguments into the varargs array component
        Class<?> varArgType = types[fixedCount].getComponentType();
        int varArgLen = args.length - fixedCount;
        Object varArgsArray = java.lang.reflect.Array.newInstance(varArgType, varArgLen);
        for (int i = 0; i < varArgLen; i++) {
            java.lang.reflect.Array.set(varArgsArray, i, args[fixedCount + i]);
        }
        result[fixedCount] = varArgsArray;
        
        return result;
    }

    /**
     * ### coerceArguments
     * 
     * Implements **Guided Coercion** for the reflection boundary. It ensures that 
     * Jolk's 64-bit Longs are narrowed to match the target Java parameter types 
     * (int, short, byte) where necessary.
     * 
     * ### The Opaque Boundary
     * 
     * When a JolkClosure is coerced into a Java Functional Interface, it enters 
     * an "Opaque" state. The implementation wrappers below must execute the 
     * closure normally. Because these callbacks originate from the Java host, 
     * any {@link JolkReturnException} thrown will result in a runtime error 
     * as there is no Lexical Home on the Java side of the stack.
     */
    private static Object[] coerceArguments(Class<?>[] types, Object[] args) {
        InteropLibrary interop = InteropLibrary.getUncached();
        Object[] coerced = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Class<?> target = types[i];
            
            // TODO: Closure to Functional Interface Conversion
            if (arg instanceof JolkClosure closure) {
                coerced[i] = convertClosureToFunctionalInterface(closure, target);
                if (coerced[i] != null) continue; // Conversion successful
            }

            // Identity Unboxing: Ensure Guest identities (TruffleString, boxed primitives) 
            // are lowered to standard Java types for reflection-based dispatch.
            if (target == String.class && interop.isString(arg)) {
                try { coerced[i] = interop.asString(arg); continue; } catch (UnsupportedMessageException ignored) {}
            }
            
            // Existing coercion logic
            if (arg instanceof Long l) {
                if (target == int.class || target == Integer.class) coerced[i] = l.intValue();
                else if (target == short.class || target == Short.class) coerced[i] = l.shortValue();
                else if (target == byte.class || target == Byte.class) coerced[i] = l.byteValue();
                else if (target == double.class || target == Double.class) coerced[i] = l.doubleValue();
                else if (target == float.class || target == Float.class) coerced[i] = l.floatValue();
                else coerced[i] = arg;
            } else {
                coerced[i] = arg;
            }
        }
        return coerced;
    }

    private static Object convertClosureToFunctionalInterface(JolkClosure closure, Class<?> targetInterface) {
        var context = tolk.language.JolkLanguage.getContext();
        var truffleContext = context.env.getContext();

        // Callable<V> -> V call()
        if (targetInterface == java.util.concurrent.Callable.class) {
            return (java.util.concurrent.Callable<Object>) () -> {
                Object prev = truffleContext.enter(null);
                try {
                    return closure.execute(new Object[0]);
                } catch (Throwable t) {
                    if (t instanceof RuntimeException re) throw re;
                    if (t instanceof Error e) throw e;
                    throw new RuntimeException(t);
                } finally {
                    truffleContext.leave(null, prev);
                }
            };
        }

        // Runnable -> void run()
        if (targetInterface == java.lang.Runnable.class) {
            return (java.lang.Runnable) () -> {
                Object prev = truffleContext.enter(null);
                try {
                    closure.execute(new Object[0]);
                } catch (Throwable ignored) {
                } finally {
                    truffleContext.leave(null, prev);
                }
            };
        }

        // Predicate<T> -> boolean test(T t)
        if (targetInterface == java.util.function.Predicate.class) {
            return (java.util.function.Predicate<Object>) (t) -> {
                Object prev = truffleContext.enter(null);
                try {
                    Object result = closure.execute(new Object[]{t});
                    return result instanceof Boolean ? (Boolean) result : false;
                } catch (Exception e) {
                    return false;
                } finally {
                    truffleContext.leave(null, prev);
                }
            };
        }
        
        // Function<T,R> -> R apply(T t)
        if (targetInterface == java.util.function.Function.class) {
            return (java.util.function.Function<Object, Object>) (t) -> {
                Object prev = truffleContext.enter(null);
                try {
                    return closure.execute(new Object[]{t});
                } catch (Exception e) {
                    return null;
                } finally {
                    truffleContext.leave(null, prev);
                }
            };
        }
        
        // Consumer<T> -> void accept(T t)
                if (targetInterface == java.util.function.Consumer.class) {
            return (java.util.function.Consumer<Object>) (t) -> {
                Object prev = truffleContext.enter(null);
                try {
                    closure.execute(new Object[]{t});
                } catch (Exception e) {
                    // Ignore
                } finally {
                    truffleContext.leave(null, prev);
                }
            };
        }
        
        // Supplier<T> -> T get()
        if (targetInterface == java.util.function.Supplier.class) {
            return (java.util.function.Supplier<Object>) () -> {
                Object prev = truffleContext.enter(null);
                try {
                    return closure.execute(new Object[0]);
                } catch (Exception e) {
                    return null;
                } finally {
                    truffleContext.leave(null, prev);
                }
            };
        }
        
        // BiFunction<T,U,R> -> R apply(T t, U u)
        if (targetInterface == java.util.function.BiFunction.class) {
            return (java.util.function.BiFunction<Object, Object, Object>) (t, u) -> {
                Object prev = truffleContext.enter(null);
                try {
                    return closure.execute(new Object[]{t, u});
                } catch (Exception e) {
                    return null;
                } finally {
                    truffleContext.leave(null, prev);
                }
            };
        }
        
        // BiConsumer<T,U> -> void accept(T t, U u)
        if (targetInterface == java.util.function.BiConsumer.class) {
            return (java.util.function.BiConsumer<Object, Object>) (t, u) -> {
                Object prev = truffleContext.enter(null);
                try {
                    closure.execute(new Object[]{t, u});
                } catch (Exception e) {
                    // Ignore
                } finally {
                    truffleContext.leave(null, prev);
                }
            };
        }
        
        // BiPredicate<T,U> -> boolean test(T t, U u)
        if (targetInterface == java.util.function.BiPredicate.class) {
            return (java.util.function.BiPredicate<Object, Object>) (t, u) -> {
                Object prev = truffleContext.enter(null);
                try {
                    Object result = closure.execute(new Object[]{t, u});
                    return result instanceof Boolean ? (Boolean) result : false;
                } catch (Exception e) {
                    return false;
                } finally {
                    truffleContext.leave(null, prev);
                }
            };
        }
        
        // Not a supported functional interface
        return null;
    }

    /// Attempts to convert arguments for a method call by inspecting the method signature via reflection.
    @TruffleBoundary
    private static Object[] convertArgumentsForMethod(Object receiver, String methodName, Object[] arguments) {
        try {
            Object unwrapped = JolkNode.unwrap(receiver);
            if (unwrapped == null) return null;
            Class<?> lookupClass = (unwrapped instanceof Class<?> clazz) ? clazz : unwrapped.getClass();
            for (java.lang.reflect.Method m : lookupClass.getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == arguments.length) {
                    Class<?>[] paramTypes = m.getParameterTypes();
                    return coerceArguments(paramTypes, arguments);
                }
            }
        } catch (SecurityException e) {
            // Reflection may be restricted
        }
        return null; // No conversion applied
    }
}