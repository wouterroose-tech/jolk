package jolk.test.engine;

import java.nio.file.Path;

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
    // find all test methods
    // create a TestDescriptor for each test method
    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {

        // 1. Create the persistent Truffle Context
        JolkTestRuntimeContext runtimeContext = new JolkTestRuntimeContext();
        
        // 2. Load the Jolk test framework classes into the context
        runtimeContext.loadDirectory("/jolk/test/api");

        // 3. Scan and evaluate user space file selectors
        JolkEngineDescriptor rootDescriptor = new JolkEngineDescriptor(uniqueId, runtimeContext);
        discoveryRequest
            .getSelectorsByType(FileSelector.class)
            .stream()
            .map(FileSelector::getPath)
            .filter(path -> path.toString().endsWith(".jolk"))
            .map(path -> classDescriptor(runtimeContext, uniqueId, path))
            .forEach(d -> rootDescriptor.addChild(d));

        // 4. Scan and evaluate user space directory selectors
        discoveryRequest
            .getSelectorsByType(DirectorySelector.class)
            .stream()
            .map(DirectorySelector::getPath)
            .flatMap(dirPath -> runtimeContext.scanDirectoryForJolkSources(dirPath).stream())
            .map(path -> classDescriptor(runtimeContext, uniqueId, path))
            .forEach(d -> rootDescriptor.addChild(d));

        // 5. Handle classpath roots (e.g., Maven/Gradle test execution over target/test-classes)
        /*
        discoveryRequest
            .getSelectorsByType(ClasspathRootSelector.class)
            .stream()
            .map(s -> s.getClasspathRoot())
            .map(p -> Path.of(p))
            .flatMap(rootPath -> runtimeContext.scanDirectoryForJolkSources(rootPath).stream())
            .map(path -> classDescriptor(runtimeContext, uniqueId, path))
            .forEach(d -> rootDescriptor.addChild(d));
        */
        return rootDescriptor;
    }

    @Override
    protected JolkTestEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        TestDescriptor root = request.getRootTestDescriptor();
        JolkTestRuntimeContext runtimeContext = ((JolkEngineDescriptor) root).getRuntimeContext();
        return new JolkTestEngineExecutionContext(runtimeContext);
    }

    JolkClassTestDescriptor classDescriptor(JolkTestRuntimeContext context, UniqueId baseId, Path filePath) {
        
        JolkMetaClass metaClass = context.evaluateJolkSource(filePath);
        UniqueId directoryDescriptorId = baseId.append("directory", filePath.getParent().toString());
        UniqueId classDescriptorId = directoryDescriptorId.append("class", (String) metaClass.getMetaSimpleName());
        JolkClassTestDescriptor classDescriptor = new JolkClassTestDescriptor(classDescriptorId, metaClass);
        // scan the MetaClass via the meta-layer protocol to identify tests
        context
            .getTestSelectors(metaClass)
            .map(s -> methodDescriptor(classDescriptorId, s))
            .forEach(d -> classDescriptor.addChild(d));
        return classDescriptor;
    }

    JolkMethodTestDescriptor methodDescriptor(UniqueId classDescriptorId, String selectorName) {
        UniqueId methodDescriptorId = classDescriptorId.append("method", selectorName);
        return new JolkMethodTestDescriptor( methodDescriptorId, selectorName, selectorName );
    }

}