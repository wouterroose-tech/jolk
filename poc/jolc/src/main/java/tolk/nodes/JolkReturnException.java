package tolk.nodes;

import com.oracle.truffle.api.nodes.ControlFlowException;

/**
 * ### JolkReturnException
 * 
 * A control-flow exception used to implement Jolk's non-local returns (`^`).
 */
public final class JolkReturnException extends ControlFlowException {
    private static final long serialVersionUID = 1L;
    private final Object result;
    private final Object target;

    public JolkReturnException(Object result, Object target) {
        this.result = result;
        this.target = target;
    }

    public Object getResult() { return result; }
    public Object getTarget() { return target; }
}