package jolk.test.engine;

import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;

import tolk.runtime.JolkMetaClass;

public class JolkTestEngineExecutionContext implements EngineExecutionContext {
    
    private final JolkTestRuntimeContext runtimeContext;

    public JolkTestEngineExecutionContext(JolkTestRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    public JolkTestRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    public Exception translateToHostException(Throwable guestException) {
        return (RuntimeException) guestException;
    }

    public Object invokeJolkTestRunner(JolkMetaClass guestTestClass, String selector) {
        return runtimeContext.invokeJolkTestRunner(guestTestClass, selector);
    }

    public boolean isSuccess(Object jolkTestResult) {
        return runtimeContext.isSuccess(jolkTestResult);
    }

}
