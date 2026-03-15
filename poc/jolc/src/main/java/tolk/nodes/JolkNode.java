package tolk.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

/**
 * The root of all Jolk execution nodes.
 * <p>
 * This class extends the Truffle {@link Node} and provides the basic structure for the Abstract
 * Syntax Tree (AST) of the Jolk language. All specific Jolk nodes (for expressions, statements,
 * etc.) will extend this class.
 * <p>
 * The "execute" methods are the core of the Truffle execution model. They are called to evaluate
 * a node in the AST. We provide a generic {@link #executeGeneric} and specialized versions for
 * primitive types to allow for type-specific optimizations by the GraalVM compiler.
 */
@TypeSystemReference(JolkTypes.class)
@NodeInfo(language = "Jolk Language", description = "The abstract base node for all Jolk AST nodes")
public abstract class JolkNode extends Node {

    /**
     * The primary execution method for a node. This is the most general version, returning an
     * {@link Object}. Subclasses must implement this method to define their execution logic.
     *
     * @param frame The current execution frame, which holds local variables.
     * @return The result of executing this node.
     */
    public abstract Object executeGeneric(VirtualFrame frame);

}
