package jolk.test.engine;

import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;

public class JolkTestEngineExecutionContext implements EngineExecutionContext {
    
    private final JolkTestRuntimeContext runtimeContext;

    public JolkTestEngineExecutionContext(JolkTestRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    public JolkTestRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

}
