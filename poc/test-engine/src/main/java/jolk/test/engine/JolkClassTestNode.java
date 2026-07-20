package jolk.test.engine;

import org.junit.platform.engine.support.hierarchical.Node;
import org.opentest4j.AssertionFailedError;

import com.oracle.truffle.api.interop.InteropLibrary;

public class JolkClassTestNode implements Node<JolkTestEngineExecutionContext> {
    
    private final JolkClassTestDescriptor descriptor;

    public JolkClassTestNode(JolkClassTestDescriptor descriptor) {
        this.descriptor = descriptor;
    }
    
    @Override
    public JolkTestEngineExecutionContext before(JolkTestEngineExecutionContext context) throws Exception {
        Object guestTestClass = descriptor.getGuestTestClass();
        JolkTestRuntimeContext runtimeContext = context.getRuntimeContext();
        InteropLibrary interop = InteropLibrary.getUncached();

        // Host-orchestrated Class Setup Phase (#beforeAll)
        // Invokes static initialisation message on the class definition object
        try {
            runtimeContext.invokeMember(guestTestClass, "#beforeAll");
        } catch (Exception e) {
            throw new AssertionFailedError("Class-level setup failed", e);
        }
        return context;
    }

    @Override
    public JolkTestEngineExecutionContext execute(
            JolkTestEngineExecutionContext context, 
            DynamicTestExecutor dynamicTestExecutor) throws Exception {
        
        // The container node itself requires no execution payload logic.
        // It returns the context unaltered to signal the engine to process child nodes.
        return context;
    }

    @Override
    public void after(JolkTestEngineExecutionContext context) throws Exception {
        Object guestTestClass = descriptor.getGuestTestClass();
        JolkTestRuntimeContext runtimeContext = context.getRuntimeContext();

        // Host-orchestrated Class Teardown Phase (#afterAll)
        try {
            runtimeContext.invokeMember(guestTestClass, "#afterAll");
        } catch (Exception e) {
            throw new AssertionFailedError("Class-level teardown failed", e);
        }
    }
}