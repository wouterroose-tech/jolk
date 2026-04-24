package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import tolk.runtime.JolkClosure;
/**
 * ### JolkTernaryNode
 * 
 * Optimized AST node for the ternary operator (? :). 
 * It manually evaluates branches to preserve laziness while bypassing 
 * message dispatch and closure allocation.
 */
public class JolkTernaryNode extends JolkExpressionNode {

    @Child private JolkNode conditionNode;
    @Child private JolkNode thenNode;
    @Child private JolkNode elseNode;
    private final boolean negate;

    public JolkTernaryNode(boolean negate, JolkNode conditionNode, JolkNode thenNode, JolkNode elseNode) {
        this.negate = negate;
        this.conditionNode = conditionNode;
        this.thenNode = thenNode;
        this.elseNode = elseNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        boolean condition;
        try {
            condition = conditionNode.executeBoolean(frame);
        } catch (UnexpectedResultException e) {
            Object res = e.getResult();
            if (res instanceof Boolean b) condition = b;
            else throw new RuntimeException("Ternary condition must be Boolean");
        }

        boolean actualCondition = negate ? !condition : condition;
        Object branchResult = actualCondition ? thenNode.executeGeneric(frame) : elseNode.executeGeneric(frame);

        // If the branch result is a JolkClosure, execute it to get the actual value
        if (branchResult instanceof JolkClosure closure) {
            return closure.execute(new Object[0]);
        }
        return branchResult;
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        boolean cond;
        try {
            cond = conditionNode.executeBoolean(frame);
        } catch (UnexpectedResultException e) {
            Object res = e.getResult();
            if (res instanceof Boolean b) cond = b;
            else throw new UnexpectedResultException(res);
        }
        
        boolean actual = negate ? !cond : cond;
        
        JolkNode branch = actual ? thenNode : elseNode;

        try {
            return branch.executeLong(frame);
        } catch (UnexpectedResultException e) {
            Object branchResult = e.getResult();
            if (branchResult instanceof JolkClosure closure) {
                Object closureExecutionResult = closure.execute(new Object[0]);
                if (closureExecutionResult instanceof Long l) return l;
                throw new UnexpectedResultException(closureExecutionResult);
            }
            throw e;
        }
    }
}