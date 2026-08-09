package jolk.test.engine;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;
import org.opentest4j.TestSkippedException;

import com.oracle.truffle.api.interop.InteropLibrary;

///
/// Represents an individual test method selector (leaf node)
/// 
/// @author Wouter Roose
///
public class JolkMethodTestDescriptor extends AbstractTestDescriptor implements Node<JolkTestEngineExecutionContext> {

    // The message selector string (e.g., "#testMethod")
    private final String selector;

    public JolkMethodTestDescriptor(UniqueId uniqueId, String selector, TestSource source) {
        super(uniqueId, selector, source);
        this.selector = selector;
    }

    @Override
    public Type getType() {
        return Type.TEST; // Allows lazy hook attachments or plain execution
    }
    
    @Override
    public JolkTestEngineExecutionContext execute(
            JolkTestEngineExecutionContext context, 
            DynamicTestExecutor dynamicTestExecutor) throws Exception {

        // Resolve parent container reference to access the guest MetaClass
        JolkClassTestDescriptor parent = getParent()
            .filter(JolkClassTestDescriptor.class::isInstance)
            .map(JolkClassTestDescriptor.class::cast)
            .orElseThrow(() -> new IllegalStateException(
                    "Parent descriptor for " + getUniqueId() + " is not a valid JolkClassTestDescriptor"));

        Value guestTestClass = parent.getGuestTestClass();

        // Explicitly enter the Truffle context for the duration of the interop dispatches
        //polyglotContext.enter();

        // Execution the guest Jolk validation logic:
        // TestResult result = testClass #new #selector(s) #run;
        Value testInstance;
        try {
            testInstance = guestTestClass.invokeMember("new");
        } catch (Throwable guestException) {
            throw new AssertionFailedError("Failed to instantiate test class", guestException);
        }
        try {
            // Orchestrate the lifecycle phases via independent host-to-guest boundaries
            testInstance.invokeMember("beforeEach");
            // Invoke the target test method explicitly
            testInstance.invokeMember(selector);
            return context;
        } catch (Throwable throwable) {
            if (throwable instanceof PolyglotException polyglotException) {
                String message = polyglotException.getMessage();
                if (message != null && message.contains("DISABLED")) {
                    throw new TestSkippedException("Test disabled: " + message, polyglotException);
                }
                if (message != null && message.contains("FAILURE")) {
                    throw new AssertionFailedError("Failed to execute test method: " + message, polyglotException);
                }
                throw new TestAbortedException("Test failed: " + message, polyglotException);
            }
            throw throwable;
        } finally {
            // Guarantee resource reclamation regardless of test execution outcome
            testInstance.invokeMember("afterEach");
        }
    }
    
}
