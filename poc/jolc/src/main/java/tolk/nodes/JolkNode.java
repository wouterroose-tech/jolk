package tolk.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.nodes.NodeInfo;
import tolk.runtime.JolkNothing;

/// # JolkNode (The AST Substrate)
///
/// The abstract base for all identities within the Jolk Abstract Syntax Tree. 
/// It provides the structural scaffolding for the Truffle execution model, 
/// enforcing the type system via {@link JolkTypes}.
/// 
/// This class implements the core protocols for **Identity Restitution** (lifting) 
/// and **Impedance Resolution** (unwrapping), ensuring that the boundary between 
/// the guest Jolk environment and the host JVM remains semantically secure.
@TypeSystemReference(JolkTypes.class)
@NodeInfo(language = "Jolk Language", description = "The abstract base node for all Jolk AST nodes")
public abstract class JolkNode extends Node {

    /// The primary execution method for a node. This is the most general version, returning an
    /// `Object`. Subclasses must implement this method to define their execution logic.
    ///
    /// @param frame The current execution frame, which holds local variables.
    /// @return The result of executing this node.
    public abstract Object executeGeneric(VirtualFrame frame);

    /// Specialized execution for primitive longs.
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return JolkTypesGen.expectLong(executeGeneric(frame));
    }

    /// Specialized execution for primitive booleans.
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return JolkTypesGen.expectBoolean(executeGeneric(frame));
    }

    public TruffleString executeTruffleString(VirtualFrame frame) throws UnexpectedResultException {
        return JolkTypesGen.expectTruffleString(executeGeneric(frame));
    }

    /// Performs "Identity Restitution" by lifting a potential raw Java null into the 
    /// Jolk {@link JolkNothing#INSTANCE}. This ensures that even uninitialized states 
    /// or nulls passed from the Java host can safely participate in Jolk's unified 
    /// message-passing protocol.
    /// 
    /// @param value The value to lift.
    /// @return The lifted value (either the original object or JolkNothing.INSTANCE).
    public static Object lift(Object value) {
        if (value == null) return JolkNothing.INSTANCE;

        // IDENTITY RESTITUTION: Ensure host types match Jolk guest currency.
        // Integer to Long (common in interop) and String to TruffleString.
        if (value instanceof Long || value instanceof Boolean || value instanceof TruffleString || value instanceof JolkNothing) {
            return value;
        }
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof String s) {
            return TruffleString.fromJavaStringUncached(s, TruffleString.Encoding.UTF_16);
        }

        return liftSlow(value);
    }

    @TruffleBoundary
    private static Object liftSlow(Object value) {
        try {
            if (InteropLibrary.getUncached().isNull(value)) {
                return JolkNothing.INSTANCE;
            }
            // Identity Restitution: Ensure host objects are wrapped for Interop consistency.
            return tolk.language.JolkLanguage.getContext().env.asGuestValue(value);
        } catch (AssertionError | IllegalStateException e) {
            // Handled: Context-less execution path (e.g. low-level runtime unit tests)
            return value;
        }
    }

    /// Performs **Impedance Resolution**. If the provided value is a wrapped 
    /// Truffle Host Object, it extracts the underlying Java instance.
    /// This is used when a Jolk built-in or dispatch needs to operate on the 
    /// raw Java object behind a Truffle host wrapper.
    public final static Object unwrap(Object value) {
        try {
            // Impedance Resolution: Use the environment to extract the raw Java object.
            // We catch AssertionError/IllegalStateException to allow this to run 
            // in context-less unit tests where no polyglot context is entered.
            var context = tolk.language.JolkLanguage.getContext();
            if (context != null && context.env.isHostObject(value)) {
                return context.env.asHostObject(value);
            }
        } catch (AssertionError | IllegalStateException e) {
            // Handled: Context-less execution path
        }
        return value;
    }

    /**
     * ### isNothing
     * 
     * High-performance guard to identify Jolk's absence identity.
     */
    protected static boolean isNothing(Object receiver) {
        return receiver == null || receiver == JolkNothing.INSTANCE;
    }

    /// Navigates the lexical environment chain to find the arguments array at the specified depth.
    /// This is the standard mechanism in Jolk for environment traversal.
    /// 
    /// @param frame The starting frame.
    /// @param depth The number of levels to traverse.
    /// @return The arguments array of the target environment, or null if unreachable.
    @ExplodeLoop
    protected final Object[] getTargetArgs(VirtualFrame frame, int depth) {
        Object[] current = frame.getArguments();
        for (int i = 0; i < depth; i++) {
            if (current != null && current.length > 0) {
                Object env = current[0];
                current = (env instanceof Frame f) ? f.getArguments() : 
                          (env instanceof Object[] oa ? oa : null);
            } else {
                return null;
            }
        }
        return current;
    }

    /// Navigates the lexical environment chain to find the Frame at the specified depth.
    /// 
    /// @param frame The starting frame.
    /// @param depth The number of levels to traverse.
    /// @return The target Frame, or null if unreachable.
    @ExplodeLoop
    protected final Frame getTargetFrame(VirtualFrame frame, int depth) {
        Frame current = frame;
        for (int i = 0; i < depth; i++) {
            Object[] args = current.getArguments();
            if (args.length > 0 && args[0] instanceof Frame next) {
                current = next;
            } else {
                return null;
            }
        }
        return current;
    }
}
