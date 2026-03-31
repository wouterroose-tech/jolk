package tolk.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;

/// The root of all Jolk execution nodes.
///
/// This class extends the Truffle `Node` and provides the basic structure for the Abstract
/// Syntax Tree (AST) of the Jolk language. All specific Jolk nodes (for expressions, statements,
/// etc.) will extend this class.
///
/// The "execute" methods are the core of the Truffle execution model. They are called to evaluate
/// a node in the AST. We provide a generic `executeGeneric` and specialized versions for
/// primitive types to allow for type-specific optimizations by the GraalVM compiler.
@TypeSystemReference(JolkTypes.class)
@NodeInfo(language = "Jolk Language", description = "The abstract base node for all Jolk AST nodes")
public abstract class JolkNode extends Node {

    /// The primary execution method for a node. This is the most general version, returning an
    /// `Object`. Subclasses must implement this method to define their execution logic.
    ///
    /// @param frame The current execution frame, which holds local variables.
    /// @return The result of executing this node.
    public abstract Object executeGeneric(VirtualFrame frame);

    /**
     * Navigates the lexical environment chain to find the arguments array at the specified depth.
     * This is the standard mechanism in Jolk for environment traversal.
     * 
     * @param frame The starting frame.
     * @param depth The number of levels to traverse.
     * @return The arguments array of the target environment, or null if unreachable.
     */
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
}
