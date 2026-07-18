package jolk.test.engine;

import java.nio.file.Path;
import java.util.List;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

import tolk.language.JolkLanguage;
import tolk.runtime.JolkMetaClass;

public class JolkTestEngine extends HierarchicalTestEngine<JolkTestEngineExecutionContext> {

    @Override
    public String getId() {
        return JolkLanguage.ID;
    }

    // discover all .jolk files in src/test/jolk
    // load each file as a Jolk class via context.getJolkClass() 
    // find all methods returning jolk.test.api.Test without parameters
    // and create a TestDescriptor for each method
    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {

        // 1. Create the persistent Truffle Context
        JolkTestRuntimeContext runtimeContext = new JolkTestRuntimeContext();
        
        // 2. Load the core test framework first so the TestCase protocol exists
        runtimeContext.loadTestFramework();
        JolkEngineDescriptor rootDescriptor = new JolkEngineDescriptor(uniqueId, runtimeContext);

        // 3. Scan and evaluate user space file selectors
        discoveryRequest.getSelectorsByType(FileSelector.class).forEach(selector -> {
            Path filePath = selector.getPath();
            if (filePath.toString().endsWith(".jolk")) {
                evaluateAndAppendTestDescriptor(filePath, rootDescriptor, runtimeContext, uniqueId);
            }
        });

        // 4. Scan and evaluate user space directory selectors
        discoveryRequest.getSelectorsByType(DirectorySelector.class).forEach(selector -> {
            Path dirPath = selector.getPath();// Implement file-tree walk filtering for *.jolk files
            List<Path> discoveredFiles = runtimeContext.scanDirectoryForJolkSources(dirPath);
            for (Path filePath : discoveredFiles) {
                evaluateAndAppendTestDescriptor(filePath, rootDescriptor, runtimeContext, uniqueId);
            }
        });
        return rootDescriptor;
    }

    @Override
    protected JolkTestEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        TestDescriptor root = request.getRootTestDescriptor();
        JolkTestRuntimeContext runtimeContext = ((JolkEngineDescriptor) root).getRuntimeContext();
        return new JolkTestEngineExecutionContext(runtimeContext);
    }

    private void evaluateAndAppendTestDescriptor(
        Path filePath, 
        JolkEngineDescriptor root, 
        JolkTestRuntimeContext context, 
        UniqueId baseId) {
        
        JolkMetaClass metaClass = context.evaluateJolkSource(filePath);
        // Interrogate the live MetaClass via the meta-layer protocol to check if it conforms to TestCase
        if (context.conformsToTestProtocol(metaClass)) {
            String className = context.getClassName(metaClass);
            UniqueId classDescriptorId = baseId.append("class", className);
            JolkClassTestDescriptor classDescriptor = new JolkClassTestDescriptor(classDescriptorId, className, metaClass);
            // Fetch the individual test method selector names via the guest #testProtocol message
            List<String> selectors = context.extractTestSelectors(metaClass);
            for (String selectorName : selectors) {
                UniqueId methodDescriptorId = classDescriptorId.append("method", selectorName);
                JolkMethodTestDescriptor methodDescriptor = new JolkMethodTestDescriptor(
                    methodDescriptorId, selectorName, selectorName
                );
                classDescriptor.addChild(methodDescriptor);
            }
            root.addChild(classDescriptor);
        }
    }

}