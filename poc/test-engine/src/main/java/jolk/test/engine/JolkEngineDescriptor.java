package jolk.test.engine;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

///
/// Represents a Jolk test class or file container.
/// 
/// @author Wouter Roose
/// 
public class JolkEngineDescriptor extends EngineDescriptor {
    
    // Hold context dependencies here
    private final JolkTestRuntimeContext runtimeContext;

    public JolkEngineDescriptor(UniqueId uniqueId, JolkTestRuntimeContext runtimeContext) {
        super(uniqueId, "Jolk Test Engine");
        this.runtimeContext = runtimeContext;
    }

    public JolkTestRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

}
