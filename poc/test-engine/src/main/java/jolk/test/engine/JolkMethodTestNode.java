package jolk.test.engine;

import org.junit.platform.engine.support.hierarchical.Node;
import org.opentest4j.AssertionFailedError;

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
        try {
            // Resolve parent container reference to access the guest MetaClass
            JolkClassTestDescriptor parent = (JolkClassTestDescriptor) descriptor.getParent().orElseThrow();
            JolkMetaClass guestTestClass = parent.getGuestTestClass();
            String selector = descriptor.getSelector();
            // Dispatches execution directly to the  guest Jolk validation logic:
            // TestResult result = testClass #new #selector(s) #run;
            Object jolkTestResult = context.invokeJolkTestRunner(guestTestClass, selector);
            
            if (!context.isSuccess(jolkTestResult)) {
                throw new AssertionFailedError("Jolk assertion error encountered.");
            }
            return context;
        } catch (Throwable guestException) {
            // Intercepts guest-language control exceptions and maps them to host failure nodes
            throw context.translateToHostException(guestException);
        }
    }

}
