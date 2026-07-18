package jolk.test.engine;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

///
/// Represents an individual test method selector (leaf node)
/// 
/// @author Wouter Roose
///
public class JolkMethodTestDescriptor extends AbstractTestDescriptor {

    // The message selector string (e.g., "#testMethod")
    private final String selector;

    public JolkMethodTestDescriptor(UniqueId uniqueId, String displayName, String selector) {
        super(uniqueId, displayName);
        this.selector = selector;
    }

    @Override
    public Type getType() {
        return Type.CONTAINER_AND_TEST; // Allows lazy hook attachments or plain execution
    }

    public String getSelector() {
        return selector;
    }
}
