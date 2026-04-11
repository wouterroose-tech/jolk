package tolk.nodes;

import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.nodes.IndirectCallNode;
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
import tolk.runtime.JolkStringExtension;
import tolk.runtime.JolkMatch;
import tolk.runtime.JolkBooleanExtension;
import tolk.runtime.JolkExceptionExtension;
import tolk.runtime.JolkArrayExtension;
import tolk.runtime.JolkLongExtension;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkIntrinsicProtocol;

/// # JolkDispatchNode
///
/// The `JolkDispatchNode` is responsible for dispatching messages (method calls)
/// to the receiver object. It acts as the gateway between the Jolk AST and the
/// object's behavior.
///
/// In Jolk, every interaction is a message send. This node handles the polymorphic
/// dispatch logic, leveraging the Truffle DSL to inline caches (ICs) for performance.
/// It primarily uses the `InteropLibrary` to interact with objects, ensuring strict
/// adherence to the defined protocols (including those of `JolkNothing`).
/// 
/// ### Specialization for `JolkNothing`
///
/// The `doNothing` specialization provides a high-performance, monomorphic "fast path" for
/// messages sent to the `JolkNothing.INSTANCE` singleton. In a message-passing system, the
/// absence of a value (`null`) is a common receiver, and optimizing for it is critical.
///
/// This specialization works by caching the `InteropLibrary` specific to the `JolkNothing`
/// type. Since `JolkNothing` is a singleton, this cache is always hit after the first
/// invocation, making subsequent dispatches extremely fast. The node simply delegates the
/// message (`invokeMember`) to the `JolkNothing` object itself, which defines how it
/// should respond to various selectors (typically by returning itself, absorbing the message).
///
/// ### Generic Dispatch and Polymorphic Inline Caches
///
/// The `doDispatch` specialization is the general-purpose, polymorphic fallback for any
/// object that is not `JolkNothing`. It leverages Truffle's Polymorphic Inline Caches (PICs)
/// to maintain high performance across different receiver types.
///
/// The `limit = "3"` directive instructs the Truffle DSL to create an inline cache that can
/// specialize for up to three distinct receiver types. Here's how it works:
///
/// 1.  **Monomorphic State**: On the first invocation with a new type (e.g., `JolkMetaClass`),
///     the node rewrites itself to include a fast `instanceof JolkMetaClass` check and caches
///     the appropriate `InteropLibrary`.
/// 2.  **Polymorphic State**: When a second or third new type is encountered, the node adds
///     more `instanceof` checks, creating a chain of fast paths.
/// 3.  **Megamorphic State**: If a fourth type is seen, the cache is considered "megamorphic."
///     The node transitions to a more generic (and slightly slower) dispatch mechanism that
///     can handle an unlimited number of types, typically using a hash map lookup on the
///     receiver's class.
///
/// This strategy ensures that common call sites with a few receiver types remain highly
/// optimized, while still correctly handling fully dynamic scenarios.
///
@GenerateInline(false)
public abstract class JolkDispatchNode extends JolkNode { // Keep extending JolkNode

    /**
     * ### create
     * 
     * Static factory method to instantiate the node implementation. This method 
     * delegates to the generated {@link JolkDispatchNodeGen} class.
     * 
     * @return An instance of the generated JolkDispatchNode.
     */
    public static JolkDispatchNode create() {
        return JolkDispatchNodeGen.create();
    }

    /// Executes the message dispatch.
    ///
    /// @param frame The current execution frame.
    /// @param receiver The object receiving the message.
    /// @param selector The message name (selector).
    /// @param arguments The arguments passed to the message.
    /// @return The result of the message send.
    public abstract Object execute(VirtualFrame frame, Object receiver, String selector, Object[] arguments);

    /**
     * This method is not intended to be called directly on a JolkDispatchNode.
     * It is marked as `final` to satisfy the requirement from {@link JolkNode} 
     * while shielding it from the Truffle DSL's specialization generator. 
     */
    @Override
    public final Object executeGeneric(VirtualFrame frame) {
        throw new UnsupportedOperationException("JolkDispatchNode is not designed to be executed directly via executeGeneric(VirtualFrame). Use execute(...) instead.");
    }

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

    protected static boolean isControlFlow(String selector) {
        return switch (selector) {
            case "ifPresent", "ifEmpty", "??", "?", "?!", "? :", "?! :", "finally" -> true;
            default -> false;
        };
    }

    protected static boolean isClosureCatch(String selector) {
        return "catch".equals(selector);
    }

    protected static boolean isClosure(Object receiver) {
        return receiver instanceof JolkClosure;
    }
    // --- End of Helper methods ---

    @Specialization(guards = {"isNothing(receiver)", "!isControlFlow(selector)", "!isClosureCatch(selector)"})
    protected Object doNothing(VirtualFrame frame, Object receiver, String selector, Object[] arguments, 
                                @CachedLibrary("getNothing()") InteropLibrary interop) {
        try {
            // Identity Restitution Protocol: For messages not handled by intrinsics, 
            // Nothing absorbs the message by returning itself. We delegate to the 
            // instance via Interop to allow for guest-level extensibility.
            return lift(interop.invokeMember(JolkNothing.INSTANCE, selector, arguments));
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException e) {
            if (JolkIntrinsicProtocol.isObjectIntrinsic(selector)) {
                return JolkIntrinsicProtocol.dispatchObjectIntrinsic(JolkNothing.INSTANCE, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(JolkNothing.INSTANCE, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Nothing", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on Nothing", e);
        }
    }

    /**
     * ### Fast Path for Shape-based Property Access
     * 
     * Specializes the dispatch for native JolkObjects. If the selector matches 
     * a property in the cached shape, it performs a direct offset load.
     */
    @Specialization(guards = {
        "receiver.getShape() == cachedShape",
        "selector == cachedSelector",
        "property != null"
    }, limit = "3")
    protected Object doShapeRead(DynamicObject receiver, String selector, Object[] arguments,
                                @Cached("receiver.getShape()") Shape cachedShape,
                                @Cached("selector") String cachedSelector,
                                @Cached("cachedShape.getProperty(selector)") Property property,
                                @CachedLibrary("receiver") DynamicObjectLibrary objLib) {

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
    /**
     * ### isNothing
     * 
     * Guard used to identify if the receiver should be treated as the Jolk 
     * Nothing identity.
     * 
     * @param receiver The object to check.
     * @return true if the receiver is null or the JolkNothing instance.
     */
    protected static boolean isNothing(Object receiver) {
        return receiver == null || receiver == JolkNothing.INSTANCE;
    }

    protected JolkNothing getNothing() {
        return JolkNothing.INSTANCE;
    }

    /**
     * ### Fast Path for Closure-based Control Flow
     * 
     * Handles #ifPresent and #ifEmpty by directly executing the JolkClosure via 
     * an IndirectCallNode. This enables Truffle to perform inlining of the 
     * closure body, significantly improving performance compared to interop execution.
     */
    @Specialization(guards = "isControlFlow(selector)")
    protected Object doControlFlow(VirtualFrame frame, Object receiver, String selector, Object[] arguments,
                                  @Shared("callNode") @Cached IndirectCallNode callNode,
                                  @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        
        Object unwrappedReceiver = unwrap(receiver);
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
                        return lift(callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment(), lift(val)}));
                    }
                    return JolkNothing.INSTANCE;
                }
                case "ifEmpty" -> {
                    if (isAbsent) {
                        return lift(callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()}));
                    }
                    return receiver;
                }
                case "??" -> {
                    if (isAbsent) {
                        return lift(callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()}));
                    }
                    return receiver;
                }
                case "?" -> {
                    if (receiver instanceof Boolean b && b) {
                        return lift(callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()}));
                    }
                    return receiver;
                }
                case "?!" -> {
                    if (receiver instanceof Boolean b && !b) {
                        return lift(callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment()}));
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
                return lift(callNode.call(branch.getCallTarget(), new Object[]{branch.getEnvironment()}));
            }
            if ("?! :".equals(selector) && unwrappedReceiver instanceof Boolean b) {
                JolkClosure branch = !b ? thenC : elseC;
                return lift(callNode.call(branch.getCallTarget(), new Object[]{branch.getEnvironment()}));
            }
        }
        // Fallback to boundary for non-closure arguments or unhandled control flow
        return JolkIntrinsicProtocol.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
    }

    /**
     * ### Fast Path for Closure Exception Handling
     * 
     * Handles #catch on JolkClosure by routing to the closure's interop protocol.
     * This enables exception handling for closures.
     */
    @Specialization(guards = {"isClosureCatch(selector)", "isClosure(receiver)"})
    protected Object doClosureCatch(VirtualFrame frame, Object receiver, String selector, Object[] arguments,
                                   @Shared("callNode") @Cached IndirectCallNode callNode,
                                   @Shared("interop") @CachedLibrary(limit = "3") InteropLibrary interop) {
        try {
            if (receiver instanceof JolkClosure riskyClosure && arguments.length == 1 && arguments[0] instanceof JolkClosure handlerClosure) {
                try {
                    // 1. Try to execute the primary closure (the risky logic)
                    return lift(callNode.call(riskyClosure.getCallTarget(), new Object[]{riskyClosure.getEnvironment()}));
                } catch (JolkReturnException e) {
                    throw e; // Non-local returns (^) must propagate past the catch block
                } catch (Throwable e) {
                    // 2. Execute the handler closure with the caught exception
                    return lift(callNode.call(handlerClosure.getCallTarget(), new Object[]{handlerClosure.getEnvironment(), lift(e)}));
                }
            }

            // Fallback: Fix Arity error by prepending receiver (expected 2: receiver + handler)
            Object[] argsWithReceiver = new Object[arguments.length + 1];
            argsWithReceiver[0] = receiver;
            System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
            return lift(interop.invokeMember(receiver, selector, argsWithReceiver));
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        }
    }

    @Specialization(guards = "isClosureCatch(selector)", replaces = "doClosureCatch")
    protected Object doCatchPassThrough(Object receiver, String selector, Object[] arguments) {
        // If the receiver is not a closure, the 'try' block already succeeded.
        return receiver;
    }

    /**
     * ### Fast Path for Long-based Iteration (#times)
     * 
     * Handles the `Long #times [closure]` message by directly executing the JolkClosure
     * via an IndirectCallNode in a loop. This enables Truffle to perform inlining of the
     * closure body, significantly improving performance compared to interop execution.
     */
    @Specialization(guards = "isTimes(selector)")
    protected Object doTimes(VirtualFrame frame, Long receiver, String selector, Object[] arguments,
                             @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure closure) {
            for (long i = 0; i < receiver; i++) {
                // Pass the environment and the current iteration number to the closure.
                callNode.call(closure.getCallTarget(), new Object[]{closure.getEnvironment(), lift(i)});
            }
            return receiver; // Return the receiver (the count) as per Jolk's Self-Return Contract for loops.
        }
        throw new RuntimeException("Invalid arguments for #times: expected a single closure.");
    }

    /**
     * ### Fast Path for Array Filtering (#filter)
     * 
     * Implements the filter protocol for java.util.List. The IndirectCallNode 
     * allows Graal to inline the predicate logic.
     */
    @Specialization(guards = "isFilter(selector)")
    protected Object doFilter(VirtualFrame frame, List<?> receiver, String selector, Object[] arguments,
                              @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure predicate) {
            List<Object> result = new ArrayList<>();
            for (Object item : receiver) {
                Object matches = callNode.call(predicate.getCallTarget(), new Object[]{predicate.getEnvironment(), lift(item)});
                if (matches instanceof Boolean b && b) {
                    result.add(lift(item));
                }
            }
            return lift(result);
        }
        throw new RuntimeException("Invalid arguments for #filter: expected a single closure.");
    }

    /**
     * ### Fast Path for Array AnyMatch (#anyMatch)
     * 
     * Implements the anyMatch protocol for java.util.List. The IndirectCallNode 
     * allows Graal to inline the predicate logic. It returns true as soon as 
     * the predicate returns true for any element, otherwise false.
     */
    @Specialization(guards = "isAnyMatch(selector)")
    protected Object doAnyMatch(VirtualFrame frame, List<?> receiver, String selector, Object[] arguments,
                                @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure predicate) {
            for (Object item : receiver) {
                Object matches = callNode.call(predicate.getCallTarget(), new Object[]{predicate.getEnvironment(), lift(item)});
                if (matches instanceof Boolean b && b) {
                    return lift(true); // Found a match, return true
                }
            }
            return lift(false); // No match found
        }
        throw new RuntimeException("Invalid arguments for #anyMatch: expected a single closure.");
    }

    /**
     * ### Fast Path for Iterator Traversal (#forEach)
     * 
     * Projects the kinetic substrate of an Iterator directly into a message-passing 
     * loop. This is the implementation of the IteratorExtension.
     */
    @Specialization(guards = "isForEach(selector)")
    protected Object doIteratorForEach(VirtualFrame frame, java.util.Iterator<?> receiver, String selector, Object[] arguments,
                                       @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure action) {
            while (receiver.hasNext()) {
                callNode.call(action.getCallTarget(), new Object[]{action.getEnvironment(), lift(receiver.next())});
            }
            return JolkNothing.INSTANCE;
        }
        throw new RuntimeException("Invalid arguments for #forEach: expected a single closure.");
    }

    /**
     * ### Fast Path for Map Iteration (#forEach)
     * 
     * Maps the associative archetype to a two-argument closure.
     */
    @Specialization(guards = "isForEach(selector)")
    protected Object doMapForEach(VirtualFrame frame, Map<?, ?> receiver, String selector, Object[] arguments,
                                  @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure action) {
            for (Map.Entry<?, ?> entry : receiver.entrySet()) {
                // Jolk Map #forEach sends (key, value) to the closure
                callNode.call(action.getCallTarget(), new Object[]{
                    action.getEnvironment(), 
                    lift(entry.getKey()), 
                    lift(entry.getValue())
                });
            }
            return lift(receiver);
        }
        throw new RuntimeException("Invalid arguments for #forEach: expected a single closure.");
    }

    /**
     * ### Fast Path for Array Projection (#map)
     * 
     * Implements the Stream Protocol for java.util.List. While this looks like 
     * it creates an intermediate list, Graal's Partial Escape Analysis (PEA) 
     * and Loop Fusion will "boil away" the intermediate allocation when 
     * messages are chained, resulting in zero-overhead single-pass execution.
     */
    @Specialization(guards = "isMap(selector)")
    protected Object doMap(VirtualFrame frame, List<?> receiver, String selector, Object[] arguments,
                           @Shared("callNode") @Cached IndirectCallNode callNode) {
        if (arguments.length == 1 && arguments[0] instanceof JolkClosure mapper) {
            List<Object> result = new ArrayList<>(receiver.size());
            for (Object item : receiver) {
                // The IndirectCallNode allows Graal to inline the closure logic 
                // directly into this loop.
                Object projected = callNode.call(mapper.getCallTarget(), new Object[]{mapper.getEnvironment(), lift(item)});
                result.add(projected);
            }
            return lift(result);
        }
        throw new RuntimeException("Invalid arguments for #map: expected a single closure.");
    }

    /// ### Fast Path for Longs
    /// Handles raw Java Longs by routing messages to the JolkLong prototype.
    @Specialization
    protected Object doLong(VirtualFrame frame, Long receiver, String selector, Object[] arguments, 
                           @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        try {
            // 1. Prototype Lookup: Check for Jolk-defined arithmetic extensions
            Object member = JolkLongExtension.LONG_TYPE.lookupInstanceMember(selector);
            if (member != null) {
                Object[] argsWithReceiver = new Object[arguments.length + 1];
                argsWithReceiver[0] = receiver;
                if (arguments.length > 0) {
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                }
                // Library Mismatch: Use a generic library to execute members on a specialized receiver.
                return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
            }
            
            // Identity-Based Flow Control: #?? on a non-null Long always returns the receiver.
            if (arguments.length == 1 && "??".equals(selector)) return receiver;

            // 2. Host Fallback: Dispatch to standard Java Long members (e.g. longValue, compareTo)
            return lift(interop.invokeMember(receiver, selector, arguments));

        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException e) {
            if (JolkIntrinsicProtocol.isObjectIntrinsic(selector)) {
                return JolkIntrinsicProtocol.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Long", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on Long", e);
        }
    }

    /// ### Fast Path for Booleans
    /// Handles raw Java Booleans by routing messages to the JolkBoolean prototype. This ensures
    /// that boolean primitives can participate in Jolk's message-passing protocol.
    @Specialization
    protected Object doBoolean(VirtualFrame frame, Boolean receiver, String selector, Object[] arguments, 
                               @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        try {
            // 1. Prototype Lookup: Check for Jolk-defined boolean extensions
            Object member = JolkBooleanExtension.BOOLEAN_TYPE.lookupInstanceMember(selector);
            if (member != null) {
                Object[] argsWithReceiver = new Object[arguments.length + 1];
                argsWithReceiver[0] = receiver;
                if (arguments.length > 0) {
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                }
                // Library Mismatch: Use a generic library to execute members on a specialized receiver.
                return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
            }

            // Identity-Based Flow Control: #?? on a non-null Boolean always returns the receiver.
            if (arguments.length == 1 && "??".equals(selector)) return receiver;

            // 2. Host Fallback: Dispatch to standard Java Boolean members (if any)
            return lift(interop.invokeMember(receiver, selector, arguments));

        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException e) {
            if (JolkIntrinsicProtocol.isObjectIntrinsic(selector)) {
                return JolkIntrinsicProtocol.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Boolean", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiver, e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on Boolean", e);
        }
    }

    /// ### doString
    /// 
    /// Specialized **Fast Path** for host strings.
    /// 
    /// This specialization allows [java.lang.String] instances to participate 
    /// in the Jolk messaging protocol by prioritizing Jolk-native string 
    /// augmentations (defined in jolk.lang.String) before falling back to 
    /// standard Java methods.
    @Specialization
    protected Object doString(VirtualFrame frame, String receiver, String selector, Object[] arguments,
                             @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        try {
            // Shim-less Optimization: Direct dispatch for common String methods 
            // where Interop might treat the receiver as a value rather than an object.
            if (arguments.length == 0) {
                if ("length".equals(selector)) return (long) receiver.length();
                if ("isEmpty".equals(selector)) return receiver.isEmpty();
                if ("toUpperCase".equals(selector)) return lift(receiver.toUpperCase());
                if ("toLowerCase".equals(selector)) return lift(receiver.toLowerCase());
                if ("trim".equals(selector)) return lift(receiver.trim());
            }
            // Identity-Based Flow Control: #?? optimization for non-null Strings
            if (arguments.length == 1 && "??".equals(selector)) return receiver;

            if (arguments.length == 1 && "contains".equals(selector)) {
                Object arg = arguments[0];
                if (interop.isString(arg)) return receiver.contains(interop.asString(arg));
            }

            // 1. Prototype Lookup: Check for Jolk-defined string extensions (e.g., #match)
            Object member = JolkStringExtension.STRING_TYPE.lookupInstanceMember(selector);
            if (member != null) {
                Object[] argsWithReceiver = new Object[arguments.length + 1];
                argsWithReceiver[0] = receiver;
                if (arguments.length > 0) {
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                }
                return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
            }

            // 2. Host Fallback: Dispatch to standard java.lang.String members
            return lift(interop.invokeMember(receiver, selector, arguments));
        } catch (UnknownIdentifierException e) {
            if (JolkIntrinsicProtocol.isObjectIntrinsic(selector)) {
                return JolkIntrinsicProtocol.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on String", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on String", e);
        }
    }

    /// ### Fast Path for Lists
    /// Handles java.util.List instances by routing messages to the Jolk Array extension.
    @Specialization
    protected Object doList(VirtualFrame frame, List<?> receiver, String selector, Object[] arguments,
                           @CachedLibrary(limit = "3") @Shared("interop") InteropLibrary interop) {
        try {
            // 1. Prototype Lookup: Check for Jolk-defined Array extensions (e.g., #at, #put)
            Object member = JolkArrayExtension.ARRAY_TYPE.lookupInstanceMember(selector);
            if (member != null) {
                Object[] argsWithReceiver = new Object[arguments.length + 1];
                argsWithReceiver[0] = lift(receiver); // Identity Restitution
                if (arguments.length > 0) {
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                }
                return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
            }

            // 2. Host Fallback: Dispatch to standard java.util.List members
            return lift(interop.invokeMember(receiver, selector, arguments));
        } catch (UnknownIdentifierException e) {
            if (JolkIntrinsicProtocol.isObjectIntrinsic(selector)) {
                return JolkIntrinsicProtocol.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on List", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            throw new RuntimeException("Message dispatch failed: #" + selector + " on List", e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing #" + selector + " on List", e);
        }
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
            return lift(result);
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnknownIdentifierException e) {
            if (JolkIntrinsicProtocol.isObjectIntrinsic(selector)) {
                return JolkIntrinsicProtocol.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
            }
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Throwable", e);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            try {
                return lift(dispatchHostMember(receiver, selector, arguments));
            } catch (UnknownIdentifierException ex) {
                throw new RuntimeException("Message dispatch failed: #" + selector + " on Throwable", e);
            }
        }
    }

    /// ### Generic Dispatch
    ///
    /// This is the fallback for any object that is not `JolkNothing`. It uses a polymorphic
    /// inline cache (`limit = "3"`) to handle different receiver types efficiently.
    @Specialization(replaces = {"doNothing", "doTimes", "doMap", "doFilter", "doAnyMatch", "doIteratorForEach", "doMapForEach", "doControlFlow", "doLong", "doBoolean", "doString", "doList", "doThrowable"}, limit = "3") // Maps to execute
    protected Object doDispatch(VirtualFrame frame, Object receiver, String selector, Object[] arguments,
                                @CachedLibrary("receiver") InteropLibrary interop) {
        InteropLibrary uncached = InteropLibrary.getUncached();
        Object unwrappedReceiver = unwrap(receiver); // Unwrap the receiver for type checks
        try {
            // Receiver Restitution: Handle raw Java null or Interop null as Jolk Nothing identity.
            if (unwrappedReceiver == null || uncached.isNull(unwrappedReceiver)) {
                if (JolkIntrinsicProtocol.isObjectIntrinsic(selector)) {
                    return JolkIntrinsicProtocol.dispatchObjectIntrinsic(JolkNothing.INSTANCE, selector, arguments, uncached);
                }
                try {
                    return lift(uncached.invokeMember(JolkNothing.INSTANCE, selector, arguments));
                } catch (UnknownIdentifierException e) {
                    // Identity Restitution: Fallback to host member heuristic for Nothing
                    return lift(dispatchHostMember(JolkNothing.INSTANCE, selector, arguments));
                }
            }

            // 3. Jolk Prototype Lookup (Megamorphic / Generic Path)
            // Check if the receiver matches a Jolk intrinsic prototype.
            JolkMetaClass meta = null;
            if (unwrappedReceiver instanceof Long || unwrappedReceiver instanceof Integer) meta = JolkLongExtension.LONG_TYPE;
            else if (unwrappedReceiver instanceof Boolean) meta = JolkBooleanExtension.BOOLEAN_TYPE;
            else if (unwrappedReceiver instanceof String) meta = JolkStringExtension.STRING_TYPE;
            else if (unwrappedReceiver instanceof List) meta = JolkArrayExtension.ARRAY_TYPE;

            if (meta != null) {
                Object member = meta.lookupInstanceMember(selector);
                if (member != null) {
                    Object[] argsWithReceiver = new Object[arguments.length + 1];
                    argsWithReceiver[0] = lift(receiver); // Identity Restitution
                    System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
                    return lift(InteropLibrary.getUncached().execute(member, argsWithReceiver));
                }
            }

            /**
             * ### Meta-Object Interceptor (#new)
             * 
             * Implements the **Unified Messaging** rule for object creation. If the receiver 
             * is a host [Class] (MetaObject) and the selector is `#new`, we map it 
             * directly to the Interop `instantiate` protocol to invoke the Java constructor.
             */
            if ("new".equals(selector) && !(receiver instanceof JolkMetaClass) && (receiver instanceof Class || interop.isMetaObject(receiver) || interop.isInstantiable(receiver))) {
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
                        return lift(interop.instantiate(receiver, arguments));
                    }
                    return lift(dispatchHostMember(receiver, selector, arguments));
                } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException | RuntimeException e) {
                    throw new RuntimeException("Failed to instantiate host object: " + receiver + " with arguments.", e);
                }
            }

            try {
                return lift(interop.invokeMember((Object) receiver, selector, arguments));
            } catch (UnknownIdentifierException e) {
                // Identity Restitution Protocol: Intrinsic messages act as a fallback 
                // for all objects that do not explicitly override them.
                if (JolkIntrinsicProtocol.isObjectIntrinsic(selector)) {
                    return JolkIntrinsicProtocol.dispatchObjectIntrinsic(receiver, selector, arguments, interop);
                }
                // Impedance Resolution: Fallback to host member heuristic
                try {
                    return lift(dispatchHostMember(receiver, selector, arguments));
                } catch (UnknownIdentifierException ex) {
                    throw new RuntimeException("Message dispatch failed: #" + selector, e);
                }
            }
        } catch (JolkReturnException e) {
            throw e;
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
            // Use the full class name to distinguish between Value wrappers and internal HostObjects.
            String receiverStr = (receiver == null) ? "null" : receiver.getClass().getName() + " [" + receiver + "]";
            throw new RuntimeException("Message dispatch failed: #" + selector + " on " + receiverStr, e);
        }
    }

    /**
     * ### isObjectIntrinsic
     *
     * Checks if a member name belongs to the Jolk Core Protocol. This method 
     * serves as the runtime implementation of the 
     * `extension ObjectExtension on java.lang.Object` declaration.
     *
     * @param member The selector name to check.
     * @return true if the selector is a Jolk intrinsic.
     */
    public static boolean isObjectIntrinsic(String member) {
        if (member == null) return false;
        return switch (member) {
            case "new", "catch", "finally", "==", "!=", "~~", "!~", "??", "hash", "toString", "class", 
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
                    if (receiver instanceof String s1 && other instanceof String s2) {
                        return s1.equals(s2);
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
                    if (receiver instanceof String s1 && other instanceof String s2) {
                        return !s1.equals(s2);
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
                    if (receiver instanceof Number n1 && other instanceof Number n2) {
                        return n1.longValue() == n2.longValue();
                    }
                    return receiver.equals(other);
                }
                case "!~" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    if (receiver instanceof Number n1 && other instanceof Number n2) {
                        return n1.longValue() != n2.longValue();
                    }
                    return !receiver.equals(other);
                }
                case "??" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    // Identity-Based Flow Control: execute fallback only if receiver is Nothing.
                    if (receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver)) {
                        Object result = genericInterop.execute(arguments[0]);
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
                    return lift(receiver.toString());
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
            }
        } catch (JolkReturnException e) {
            throw e;
        } catch (Throwable e) {
            if (e instanceof JolkReturnException re) throw re;
            throw new RuntimeException("Intrinsic dispatch failed: #" + name, e);
        }
        return null;
    }

    /**
     * ### dispatchHostMember
     * 
     * Implements the **Shim-less Integration** heuristic by attempting to map a Jolk 
     * selector to a native Java member on a host object. It attempts the exact name, 
     * followed by common Java Bean patterns (get/is/set) and public field access.
     * 
     * @param receiver The host object receiving the message.
     * @param selector The Jolk selector (e.g., "name").
     * @param arguments The call arguments.
     * @return The result of the invocation or the receiver in case of a setter.
     * @throws UnknownIdentifierException If no matching host member is found.
     */
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
                    return interop.invokeMember(receiver, candidate, convertedArgs != null ? convertedArgs : arguments);
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

        try {
            // 1. Meta-Object Instantiation: If selector is #new and receiver is a Class, 
            // map directly to constructors to support Jolk's object creation protocol.
            if ("new".equals(methodName) && receiver instanceof Class<?> clazz) {
                for (java.lang.reflect.Constructor<?> c : clazz.getConstructors()) {
                    Object[] matched = matchArguments(c.getParameterTypes(), c.isVarArgs(), unboxed);
                    if (matched != null) {
                        Object[] coerced = coerceArguments(c.getParameterTypes(), matched);
                        try {
                            return lift(c.newInstance(coerced));
                        } catch (Exception e) {
                            // continue to next candidate
                        }
                    }
                }
            }

            // 2. Member Invocation: Simple heuristic for Shim-less Integration.
            // Allows Jolk to reach native methods on types like String and Long 
            // which are often treated as "memberless" values by standard Interop.
            for (java.lang.reflect.Method m : receiver.getClass().getMethods()) {
                if (m.getName().equals(methodName)) {
                    Object[] matched = matchArguments(m.getParameterTypes(), m.isVarArgs(), unboxed);
                    if (matched != null) {
                        Object[] coerced = coerceArguments(m.getParameterTypes(), matched);
                        try {
                                return lift(m.invoke(receiver, coerced));
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

    /**
     * ### matchArguments
     * 
     * Matches provided arguments against a parameter signature, handling 
     * Java varargs by wrapping trailing arguments into an array.
     */
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
     */
    private static Object[] coerceArguments(Class<?>[] types, Object[] args) {
        Object[] coerced = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Class<?> target = types[i];
            
            // TODO: Closure to Functional Interface Conversion
            if (arg instanceof JolkClosure closure) {
                coerced[i] = convertClosureToFunctionalInterface(closure, target);
                if (coerced[i] != null) continue; // Conversion successful
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
        // Predicate<T> -> boolean test(T t)
        if (targetInterface == java.util.function.Predicate.class) {
            return (java.util.function.Predicate<Object>) (t) -> {
                try {
                    Object result = closure.execute(new Object[]{t});
                    return result instanceof Boolean ? (Boolean) result : false;
                } catch (Exception e) {
                    return false;
                }
            };
        }
        
        // Function<T,R> -> R apply(T t)
        if (targetInterface == java.util.function.Function.class) {
            return (java.util.function.Function<Object, Object>) (t) -> {
                try {
                    return closure.execute(new Object[]{t});
                } catch (Exception e) {
                    return null;
                }
            };
        }
        
        // Consumer<T> -> void accept(T t)
        if (targetInterface == java.util.function.Consumer.class) {
            return (java.util.function.Consumer<Object>) (t) -> {
                try {
                    closure.execute(new Object[]{t});
                } catch (Exception e) {
                    // Ignore
                }
            };
        }
        
        // Supplier<T> -> T get()
        if (targetInterface == java.util.function.Supplier.class) {
            return (java.util.function.Supplier<Object>) () -> {
                try {
                    return closure.execute(new Object[0]);
                } catch (Exception e) {
                    return null;
                }
            };
        }
        
        // BiFunction<T,U,R> -> R apply(T t, U u)
        if (targetInterface == java.util.function.BiFunction.class) {
            return (java.util.function.BiFunction<Object, Object, Object>) (t, u) -> {
                try {
                    return closure.execute(new Object[]{t, u});
                } catch (Exception e) {
                    return null;
                }
            };
        }
        
        // BiConsumer<T,U> -> void accept(T t, U u)
        if (targetInterface == java.util.function.BiConsumer.class) {
            return (java.util.function.BiConsumer<Object, Object>) (t, u) -> {
                try {
                    closure.execute(new Object[]{t, u});
                } catch (Exception e) {
                    // Ignore
                }
            };
        }
        
        // BiPredicate<T,U> -> boolean test(T t, U u)
        if (targetInterface == java.util.function.BiPredicate.class) {
            return (java.util.function.BiPredicate<Object, Object>) (t, u) -> {
                try {
                    Object result = closure.execute(new Object[]{t, u});
                    return result instanceof Boolean ? (Boolean) result : false;
                } catch (Exception e) {
                    return false;
                }
            };
        }
        
        // Not a supported functional interface
        return null;
    }

    /**
     * Attempts to convert arguments for a method call by inspecting the method signature via reflection.
     */
    @TruffleBoundary
    private static Object[] convertArgumentsForMethod(Object receiver, String methodName, Object[] arguments) {
        try {
            // Use reflection to find the method and get parameter types
            for (java.lang.reflect.Method m : receiver.getClass().getMethods()) {
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