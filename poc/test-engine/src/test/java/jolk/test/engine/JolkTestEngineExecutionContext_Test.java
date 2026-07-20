package jolk.test.engine;

import org.junit.jupiter.api.Test;

public class JolkTestEngineExecutionContext_Test {

    @Test
    void testJolkTestEngineExecutionContext() {
        JolkTestRuntimeContext runtimeContext = new JolkTestRuntimeContext();
        JolkTestEngineExecutionContext executionContext = new JolkTestEngineExecutionContext(runtimeContext);
        assert executionContext.getRuntimeContext() == runtimeContext;
    }

}
