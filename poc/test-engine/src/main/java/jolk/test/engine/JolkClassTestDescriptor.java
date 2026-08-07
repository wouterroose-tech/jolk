package jolk.test.engine;

import org.graalvm.polyglot.Value;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

public class JolkClassTestDescriptor extends AbstractTestDescriptor implements Node<JolkTestEngineExecutionContext> {
    private final Value guestTestClass; // Reference to the guest-space MetaClass object

    public  JolkClassTestDescriptor(UniqueId uniqueId, Value guestTestClass, TestSource source) {
        super(uniqueId, (String) guestTestClass.getMetaSimpleName(), source);
        this.guestTestClass = guestTestClass;
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    public Value getGuestTestClass() {
        return guestTestClass;
    }
    
    @Override
    public JolkTestEngineExecutionContext before(JolkTestEngineExecutionContext context) throws Exception {
        // TODO 
        /*
        InteropLibrary interop = InteropLibrary.getUncached();

        // Host-orchestrated Class Setup Phase (#beforeAll)
        // Invokes static initialisation message on the class definition object
        try {
            interop.invokeMember(guestTestClass, "#beforeAll");
        } catch (Exception e) {
            throw new AssertionFailedError("Class-level setup failed", e);
        }
        */
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
        // TODO 
        /*
        InteropLibrary interop = InteropLibrary.getUncached();

        // Host-orchestrated Class Teardown Phase (#afterAll)
        try {
            interop.invokeMember(guestTestClass, "#afterAll");
        } catch (Exception e) {
            throw new AssertionFailedError("Class-level teardown failed", e);
        }
        */
    }
}
