package test.api;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

public class JolkTestEngine extends HierarchicalTestEngine<JolkTestEngineExecutionContext> {

    @Override
    public String getId() {
        return "jolk";
    }

    // discover all .jolk files in src/test/jolk
    // load each file as a Jolk class via context.getJolkClass() 
    // find all methods returning jolk.test.api.Test without parameters
    // and create a TestDescriptor for each method

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'discover'");
    }

    @Override
    protected JolkTestEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        return new JolkTestEngineExecutionContext();
    }

}
