package jolk.test.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
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

    @Override
    protected JolkTestEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        TestDescriptor root = request.getRootTestDescriptor();
        JolkTestRuntimeContext runtimeContext = ((JolkEngineDescriptor) root).getRuntimeContext();
        return new JolkTestEngineExecutionContext(runtimeContext);
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
        processFileSelectors(discoveryRequest, runtimeContext, uniqueId, rootDescriptor);
        processDirectorySelectors(discoveryRequest, runtimeContext, rootDescriptor);
        processClasspathRootSelectors(discoveryRequest, runtimeContext, rootDescriptor);

        return rootDescriptor;
    }

    /// Scan and evaluate user space file selectors
    void processFileSelectors(EngineDiscoveryRequest discoveryRequest, JolkTestRuntimeContext runtimeContext, UniqueId uniqueId, JolkEngineDescriptor rootDescriptor) {
        getSelectors(discoveryRequest, FileSelector.class)
            .map(FileSelector::getPath)
            .filter(path -> path.toString().endsWith(".jolk"))
            .forEach(path -> {
                Path parentDir = path.getParent();
                if (parentDir != null) {
                    registerFile(runtimeContext, rootDescriptor, parentDir, path);
                } else {
                    rootDescriptor.addChild(classDescriptor(runtimeContext, uniqueId, path));
                }
            });
    }

    /// Scan and evaluate user space directory selectors
    void processDirectorySelectors(EngineDiscoveryRequest discoveryRequest, JolkTestRuntimeContext runtimeContext, JolkEngineDescriptor rootDescriptor) {
        getSelectors(discoveryRequest, DirectorySelector.class)
            .map(DirectorySelector::getPath)
            .forEach(dirPath -> scanDirectory(runtimeContext, rootDescriptor, dirPath));
    }

    /// Scan and evaluate user space classpath root selectors
    /// e.g., Maven/Gradle test execution over target/test-classes
    void processClasspathRootSelectors(EngineDiscoveryRequest discoveryRequest, JolkTestRuntimeContext runtimeContext, JolkEngineDescriptor rootDescriptor) {
        getSelectors(discoveryRequest, ClasspathRootSelector.class)
            .map(s -> s.getClasspathRoot())
            .map(Path::of)
            .forEach(rootPath -> scanDirectory(runtimeContext, rootDescriptor, rootPath));
    }

    <T extends DiscoverySelector> Stream<T> getSelectors(EngineDiscoveryRequest discoveryRequest, Class<T> selectorType) {
        return discoveryRequest.getSelectorsByType(selectorType).stream();
    }

    /// Build the the sparse hierarchy (also called a pruned or virtual hierarchy)
    /// 
    /// This is the primary mechanism by which JolkTestEngine creates the execution tree
    /// for each test class. Contains only namespaces/folders that directly lead to 
    /// discovered test files. Empty intermediate packages are omitted.
     void scanDirectory(JolkTestRuntimeContext context, JolkEngineDescriptor rootDescriptor, Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        
        // Recursive Traversal: Files.walk handles deep recursive directory descent
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                  .filter(path -> path.toString().endsWith(".jolk"))
                  .sorted(Comparator.comparing(Path::toString))
                  // Branch Attachment: Process each recursively discovered file
                  .forEach(filePath -> registerFile(context, rootDescriptor, dir, filePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan directory: " + dir.toAbsolutePath(), e);
        }
    }

    private void (JolkTestRuntimeContext context, JolkEngineDescriptor rootDescriptor, Path dir, Path jolkFile) {
        Path relativePath = dir.relativize(jolkFile);
        Path parentPath = relativePath.getParent();

        TestDescriptor parentDescriptor = rootDescriptor;

        // Iterates through path segments (e.g., "api" -> "internal" -> "v1")
        // parentPath may be null if jolkFile is directly in rootDir
        if (parentPath != null) {
            for (Path segment : parentPath) {
                String segmentName = segment.toString();
                parentDescriptor = folderDescriptor(parentDescriptor, segmentName);
            }
        }

        // Attach leaf descriptor
        parentDescriptor.addChild(classDescriptor(context, parentDescriptor.getUniqueId(), jolkFile));
    }

    TestDescriptor folderDescriptor(TestDescriptor parent, String folderName) {
        return parent
            .getChildren().stream()
            .map(child -> (TestDescriptor) child)
            .filter(child -> child.getLegacyReportingName().equals(folderName))
            .findFirst()
            .orElseGet(() -> {
                UniqueId containerId = parent.getUniqueId().append("directory", folderName);
                JolkFolderTestDescriptor container = new JolkFolderTestDescriptor(containerId, folderName);
                parent.addChild(container);
                return container;
            });
    }

    JolkClassTestDescriptor classDescriptor(JolkTestRuntimeContext context, UniqueId parentId, Path filePath) {
        JolkMetaClass metaClass = context.evaluateJolkSource(filePath);
        UniqueId classDescriptorId = parentId.append("class", (String) metaClass.getMetaSimpleName());
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