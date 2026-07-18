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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'translateToHostException'");
    }

    public boolean isSuccess(Object jolkTestResult) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isSuccess'");
    }

    public Object invokeJolkTestRunner(JolkMetaClass guestTestClass, String selector) {
        // TODO Auto-generated method stub
        // Dispatches execution directly to the  guest Jolk validation logic:
        // TestResult result = testClass #new #selector(s) #run;
        throw new UnsupportedOperationException("Unimplemented method 'invokeJolkTestRunner'");
    }

}
