package jolk.test.engine;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import tolk.runtime.JolkMetaClass;

public class JolkTestEngineExecutionContext_Test {

    @Test
    void testJolkTestEngineExecutionContext() {
        JolkTestRuntimeContext runtimeContext = new JolkTestRuntimeContext();
        JolkTestEngineExecutionContext executionContext = new JolkTestEngineExecutionContext(runtimeContext);
        assert executionContext.getRuntimeContext() == runtimeContext;
    }

    @Test
    void testInvokeJolkTestRunner() {
        JolkTestRuntimeContext runtimeContext = new JolkTestRuntimeContext();
        runtimeContext.loadTestFramework();
        JolkMetaClass metaClass = runtimeContext.evaluateJolkSource(Path.of("/jolk/test/api/TestCase_Test.jolk"));
        JolkTestEngineExecutionContext executionContext = new JolkTestEngineExecutionContext(runtimeContext);
        Object result = executionContext.invokeJolkTestRunner(metaClass, "testSuccess");
        executionContext.isSuccess(result);

    }

}
