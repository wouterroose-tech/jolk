package tolk.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import tolk.runtime.JolkClosure;

/// # JolkClosureNode
///
/// Responsible for creating a [JolkClosure] at runtime.
/// It captures the current execution context (the frame arguments) to provide
/// lexical scoping within the closure logic.
@GenerateInline(false)
@GenerateCached(true)
public abstract class JolkClosureNode extends JolkNode {

    private final CallTarget callTarget;

    public JolkClosureNode(CallTarget callTarget) {
        this.callTarget = callTarget;
    }

    protected CallTarget getCallTarget() {
        return callTarget;
    }

    /**
     * ### doCached
     * 
     * Optimization: Caches the closure identity when the parent activation (frame) 
     * is stable. This is crucial for tight loops that pass closures to message 
     * sends (e.g. ternary branches), as it elides both the JolkClosure allocation 
     * and the frame materialization overhead during steady-state execution.
     */
    @Specialization(guards = {"frame != null", "frame == cachedFrame"}, limit = "3")
    protected JolkClosure doCached(VirtualFrame frame,
                                   @Cached("frame") VirtualFrame cachedFrame,
                                   @Cached("frame.materialize()") MaterializedFrame environment,
                                   @Cached("createClosure(environment)") JolkClosure cachedClosure) {
        return cachedClosure;
    }

    protected JolkClosure createClosure(MaterializedFrame environment) {
        return new JolkClosure(callTarget, environment);
    }

    @Specialization(replaces = "doCached")
    protected JolkClosure doGeneric(VirtualFrame frame) {
        MaterializedFrame environment = (frame != null) ? frame.materialize() : null;
        return new JolkClosure(callTarget, environment);
    }
}