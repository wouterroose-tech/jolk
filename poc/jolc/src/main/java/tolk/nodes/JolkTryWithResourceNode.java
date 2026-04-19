package tolk.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * ### JolkTryWithResourceNode
 * 
 * Implements the atomic `try-catch` protocol. It manages the lifecycle of a resource 
 * provider and ensures that the cleanup logic is executed even in the event of 
 * guest-language exceptions.
 * 
 * By utilizing the Truffle DSL, this node supports **Instructional Projection** 
 * for the internal message dispatch.
 */
@NodeInfo(shortName = "try")
@GenerateInline(false)
@GenerateCached(true)
@ImportStatic(JolkNode.class)
@NodeChild(value = "resourceProviderNode", type = JolkNode.class)
@NodeChild(value = "logicNode", type = JolkNode.class)
@NodeChild(value = "catchNode", type = JolkNode.class)
public abstract class JolkTryWithResourceNode extends JolkExpressionNode {

    /**
     * ### execute
     * 
     * The evaluation entry point for the try-catch node. This method is used by 
     * the Truffle DSL to orchestrate the evaluation of the resource provider, 
     * the logic body, and the catch handler.
     * 
     * @param frame The current execution frame.
     * @param resourceProviderNode The result of the resource provider child.
     * @param logicNode The result of the primary logic child.
     * @param catchNode The result of the catch handler child.
     * @return The result of the execution.
     */
    public abstract Object execute(VirtualFrame frame, Object resourceProviderNode, Object logicNode, Object catchNode);

    /**
     * ### doTry
     * 
     * Executes the try-catch logic. It uses the central dispatcher to initiate 
     * the `#try` protocol on the resource provider.
     */
    @Specialization
    @SuppressWarnings("truffle-static-method")
    protected Object doTry(VirtualFrame frame, Object resourceProviderNode, Object logicNode, Object catchNode,
                           @Cached(inline = true) JolkDispatchNode dispatchNode,
                           @CachedLibrary(limit = "3") InteropLibrary interop) {
        try {
            return dispatchNode.execute(frame, this, resourceProviderNode, "try", new Object[]{logicNode});
        } catch (JolkReturnException e) {
            throw e;
        } catch (Throwable throwable) {
            if (isNothing(catchNode)) {
                throw throwable;
            }
            try {
                // Execute the catch closure handler
                return JolkNode.lift(interop.execute(catchNode, new Object[]{JolkNode.lift(throwable)}));
            } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                throw new RuntimeException("Failed to execute catch handler", e);
            }
        }
    }
}
