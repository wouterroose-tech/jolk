package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkNothing;

/**
 * ### JolkMethodReferenceDispatchNode
 * 
 * Implements the runtime forwarding logic for method references (##).
 * 
 * This node implements the **Dual-Mode Reference** protocol:
 * 1. **Bound Mode**: For references on instances or static type methods (e.g., `String ##valueOf`).
 * 2. **Unbound Mode**: For instance methods referenced on a type (e.g., `String ##toUpperCase`).
 * 
 * It utilizes a runtime heuristic to determine the intention based on whether 
 * the captured identity responds to the message as a meta-object.
 */
@NodeInfo(shortName = "methodRef")
public final class JolkMethodReferenceDispatchNode extends JolkNode {

    private final String selector;
    @Child private JolkNode boundReceiverNode;
    @Child private JolkDispatchNode dispatchNode;

    public JolkMethodReferenceDispatchNode(String selector, JolkNode boundReceiverNode) {
        this.selector = selector;
        this.boundReceiverNode = boundReceiverNode;
        this.dispatchNode = JolkDispatchNodeGen.create();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object[] closureArgs = frame.getArguments();
        Object captured = (boundReceiverNode != null) ? boundReceiverNode.executeGeneric(frame) : null;
        
        Object receiver;
        Object[] callArgs;

        // Runtime Heuristic: Should we treat this as an unbound reference?
        if (shouldBeUnbound(captured, selector, closureArgs)) {
            // Unbound Mode: The receiver is provided dynamically as the first 
            // argument to the closure invocation (at index 1).
            receiver = closureArgs[1];
            
            // Forward remaining arguments passed to the closure (index 2 onwards).
            int count = closureArgs.length - 2;
            if (count <= 0) {
                callArgs = new Object[0];
            } else {
                callArgs = new Object[count];
                System.arraycopy(closureArgs, 2, callArgs, 0, count);
            }
        } else {
            // Bound Mode: Use the captured receiver (instance or static type).
            receiver = (captured == null) ? JolkNothing.INSTANCE : captured;
            
            // Forward arguments passed to the closure (index 1 onwards).
            int count = closureArgs.length - 1;
            if (count <= 0) {
                callArgs = new Object[0];
            } else {
                callArgs = new Object[count];
                System.arraycopy(closureArgs, 1, callArgs, 0, count);
            }
        }

        return lift(dispatchNode.execute(frame, receiver, selector, callArgs));
    }

    @TruffleBoundary
    private boolean shouldBeUnbound(Object captured, String selector, Object[] args) {
        if (captured == null) return true;
        
        // Heuristic: If we are referencing a Type, check if the Type responds 
        // to the selector as a meta-member (static method).
        InteropLibrary interop = InteropLibrary.getUncached();
        // Robust Meta-Identity Check: Recognize guest MetaClasses directly to 
        // avoid interop bootstrap issues in unit tests.
        boolean isMeta = captured instanceof JolkMetaClass || interop.isMetaObject(captured);
        if (isMeta && args.length > 1) {
            // 1. Check Jolk's specific MetaClass registry for factory methods or constants.
            Object unwrapped = JolkNode.unwrap(captured);
            if (unwrapped instanceof JolkMetaClass mc && mc.lookupMetaMember(selector) != null) {
                return false;
            }

            // 2. Shim-less Fallback: Check host class static methods via reflection.
            // We avoid interop.isMemberInvocable here because Jolk's MetaClass shim 
            // is "greedy"—it reports instance methods as invocable to support 
            // the host fallback, which would break our unbound heuristic.
            Class<?> hostClass = null;
            if (unwrapped instanceof Class<?> clazz) {
                hostClass = clazz;
            } else {
                // Robust Meta-Identity Resolution: Extract the name to map to host substrate.
                String metaName = "";
                if (unwrapped instanceof JolkMetaClass mc) {
                    metaName = mc.name;
                } else {
                    try {
                        Object nameObj = interop.getMetaSimpleName(captured);
                        metaName = interop.asString(nameObj);
                    } catch (UnsupportedMessageException ignored) {}
                }

                hostClass = switch (metaName) {
                    case "String" -> String.class;
                    case "Long", "Number", "Int" -> Long.class;
                    case "Double" -> Double.class;
                    case "Boolean" -> Boolean.class;
                    case "List", "Array", "ArrayList" -> java.util.ArrayList.class;
                    case "Map", "HashMap" -> java.util.HashMap.class;
                    default -> null;
                };
            }

            if (hostClass != null && hasStaticMethod(hostClass, selector)) {
                return false;
            }

            return true;
        }
        
        return false;
    }

    /**
     * ### hasStaticMethod
     * 
     * Performs a reflective lookup on the host class to determine if it exports 
     * a static method matching the provided selector. This is a critical fallback 
     * for meta-objects (Types) that do not explicitly export their static 
     * members via Truffle Interop.
     */
    @TruffleBoundary
    private boolean hasStaticMethod(Class<?> clazz, String name) {
        for (java.lang.reflect.Method m : clazz.getMethods()) {
            if (m.getName().equals(name) && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                return true;
            }
        }
        return false;
    }
}