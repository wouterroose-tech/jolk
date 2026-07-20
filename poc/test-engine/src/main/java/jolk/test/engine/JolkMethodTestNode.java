package jolk.test.engine;

import org.junit.platform.engine.support.hierarchical.Node;
import org.opentest4j.AssertionFailedError;

import com.oracle.truffle.api.interop.InteropLibrary;

import tolk.runtime.JolkMetaClass;

public class JolkMethodTestNode implements Node<JolkTestEngineExecutionContext> {
    private final JolkMethodTestDescriptor descriptor;

    public JolkMethodTestNode(JolkMethodTestDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public JolkTestEngineExecutionContext execute(
            JolkTestEngineExecutionContext context, 
            DynamicTestExecutor dynamicTestExecutor) throws Exception {

        // Resolve parent container reference to access the guest MetaClass
        JolkClassTestDescriptor parent = (JolkClassTestDescriptor) descriptor.getParent().orElseThrow();
        JolkMetaClass guestTestClass = parent.getGuestTestClass();
        String selector = descriptor.getSelector();
        JolkTestRuntimeContext runtimeContext = context.getRuntimeContext();
        InteropLibrary interop = InteropLibrary.getUncached();

        try {
            // Execution the guest Jolk validation logic:
            // TestResult result = testClass #new #selector(s) #run;
            Object testInstance = interop.invokeMember(guestTestClass, "new");
            // Orchestrate the lifecycle phases via independent host-to-guest boundaries
            runtimeContext.invokeMember(testInstance, "#before");
            
            // Invoke the target test method explicitly
            interop.invokeMember(testInstance, selector);
            return context;
        } catch (Throwable guestException) {
            // handle Disabled
            // handle Assertion
            // handle 
            throw context.translateToHostException(guestException);
        } finally {
            // Guarantee resource reclamation regardless of test execution outcome
            runtimeContext.invokeMember(testInstance, "#after");
        }
    }

}
