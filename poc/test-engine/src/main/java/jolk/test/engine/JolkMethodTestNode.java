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
        InteropLibrary interop = InteropLibrary.getUncached();

        // Execution the guest Jolk validation logic:
        // TestResult result = testClass #new #selector(s) #run;
        try {
            Object testInstance = interop.invokeMember(guestTestClass, "new");
            try {
                // Orchestrate the lifecycle phases via independent host-to-guest boundaries
                interop.invokeMember(testInstance, "#before");
                
                // Invoke the target test method explicitly
                interop.invokeMember(testInstance, selector);
                return context;
            } catch (Throwable guestException) {
                // handle Disabled
                // handle Assertion
                // handle 
                throw new AssertionFailedError("Failed to execute test method", guestException);
            } finally {
                // Guarantee resource reclamation regardless of test execution outcome
                interop.invokeMember(testInstance, "#after");
            }
        } catch (Throwable guestException) {
            throw new AssertionFailedError("Failed to instantiate test class", guestException);
        }
    }

}
