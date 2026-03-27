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

    public JolkReturnException(Object result) {
        this.result = result;
    }

    public Object getResult() { return result; }
}