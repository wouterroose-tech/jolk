package jolk.test.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.graalvm.polyglot.Value;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.CompositeTestSource;
import org.junit.platform.engine.support.descriptor.FilePosition;
import java.util.Arrays;
import java.util.List;
import org.junit.platform.engine.support.descriptor.FileSource;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

import tolk.language.JolkLanguage;

public class JolkTestEngine extends HierarchicalTestEngine<JolkTestEngineExecutionContext> {

    
    final JolkTestRuntimeContext runtimeContext;

    public JolkTestEngine() {
        super();
        // Create the Truffle Context
        runtimeContext = new JolkTestRuntimeContext();

        // Load the Jolk test framework classes into the context
        runtimeContext.loadDirectory("/jolk/test");
    }

    @Override
    public String getId() {
        return JolkLanguage.ID;
    }

    @Override
    protected JolkTestEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        TestDescriptor root = request.getRootTestDescriptor();
        // Traverse up the descriptor parent chain to obtain the top-level root (JolkEngineDescriptor)
        while (root.getClass() != JolkEngineDescriptor.class && root.getParent().isPresent()) {
            root = root.getParent().get();
        }
        JolkTestRuntimeContext runtimeContext = ((JolkEngineDescriptor) root).getRuntimeContext();
        return new JolkTestEngineExecutionContext(runtimeContext);
    }

    // discover all .jolk files in src/test/jolk
    // load each file as a Jolk class via context.getJolkClass() 
    // find all test methods
    // create a TestDescriptor for each test method
    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        JolkEngineDescriptor rootDescriptor = new JolkEngineDescriptor(uniqueId, runtimeContext);
        processFileSelectors(discoveryRequest, uniqueId, rootDescriptor);
        processDirectorySelectors(discoveryRequest, rootDescriptor);
        processClasspathRootSelectors(discoveryRequest, rootDescriptor);
        return rootDescriptor;
    }
    
    /// Scan and evaluate user space file selectors
    void processFileSelectors(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId, JolkEngineDescriptor rootDescriptor) {
        getSelectors(discoveryRequest, FileSelector.class)
            .map(FileSelector::getPath)
            .filter(path -> path.toString().endsWith(".jolk"))
            .forEach(path -> {
                // TODO or not? calculate rootdir here for proper test discovery
                Path parentDir = path.getParent();
                if (parentDir != null) {
                    registerFile(rootDescriptor, parentDir, path);
                } else {
                    rootDescriptor.addChild(classDescriptor(runtimeContext, uniqueId, parentDir, path));
                }
            });
    }

    /// Scan and evaluate user space directory selectors
    void processDirectorySelectors(EngineDiscoveryRequest discoveryRequest, JolkEngineDescriptor rootDescriptor) {
        getSelectors(discoveryRequest, DirectorySelector.class)
            .map(DirectorySelector::getPath)
            .forEach(dirPath -> scanDirectory(rootDescriptor, dirPath));
    }

    /// Scan and evaluate user space classpath root selectors
    /// e.g., Maven/Gradle test execution over target/test-classes
    void processClasspathRootSelectors(EngineDiscoveryRequest discoveryRequest, JolkEngineDescriptor rootDescriptor) {
        getSelectors(discoveryRequest, ClasspathRootSelector.class)
            .map(s -> s.getClasspathRoot())
            .map(Path::of)
            .forEach(rootPath -> scanDirectory(rootDescriptor, rootPath));
    }

    <T extends DiscoverySelector> Stream<T> getSelectors(EngineDiscoveryRequest discoveryRequest, Class<T> selectorType) {
        return discoveryRequest.getSelectorsByType(selectorType).stream();
    }

    /// Build the the sparse hierarchy (also called a pruned or virtual hierarchy)
    /// 
    /// This is the primary mechanism by which JolkTestEngine creates the execution tree
    /// for each test class. Contains only namespaces/folders that directly lead to 
    /// discovered test files. Empty intermediate packages are omitted.
     void scanDirectory(JolkEngineDescriptor rootDescriptor, Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        Path root = dir.toAbsolutePath();
        // Recursive Traversal: Files.walk handles deep recursive directory descent
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                  .filter(file -> file.toString().endsWith(".jolk"))
                  .sorted(Comparator.comparing(Path::toString))
                  // Branch Attachment: Process each recursively discovered file
                  .forEach(file -> registerFile(rootDescriptor, root, file));
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan directory: " + dir.toAbsolutePath(), e);
        }
    }

    private void registerFile(JolkEngineDescriptor rootDescriptor, Path root, Path file) {
        rootDescriptor.addChild(classDescriptor(runtimeContext, rootDescriptor.getUniqueId(), root,file));
    }

    JolkClassTestDescriptor classDescriptor(JolkTestRuntimeContext context, UniqueId parentId, Path root, Path file) {
        Value metaClass = context.evaluateJolkSource(file);
        
        // Derive FQCN(Fully Qualified Class Name) to ensure unique IDs across subdirectories
        // relative to classpath root (e.g., "jolk.test.api.TestCase_Test")
        String fqcn = extractClassName(root, file);
        UniqueId classDescriptorId = parentId.append("class", fqcn);
        List<TestSource> testSources = Arrays.asList(
                ClassSource.from(fqcn),
                FileSource.from(file.toAbsolutePath().toFile()));
        CompositeTestSource fileSource = CompositeTestSource.from(testSources);
        JolkClassTestDescriptor classDescriptor = new JolkClassTestDescriptor(classDescriptorId, metaClass, fileSource);

        // Line counter initialized to start at line 10
        AtomicInteger lineOffset = new AtomicInteger(1);

        // scan the MetaClass via the meta-layer protocol to identify tests
        context
            .getTestSelectors(context.getDefinedClass(metaClass)).map(selector -> methodDescriptor(
                classDescriptorId, 
                selector, 
                file, 
                lineOffset.getAndIncrement()))
            .forEach(classDescriptor::addChild);
        return classDescriptor;
    }

    JolkMethodTestDescriptor methodDescriptor(UniqueId classDescriptorId, String selectorName, Path filePath, int lineNumber) {
        // Use "test" as segment type for dynamic non-Java methods
        // to prevent vscode-java-test from forcing Java reflection resolution.
        UniqueId methodDescriptorId = classDescriptorId.append("test", selectorName);
        FilePosition position = FilePosition.from(lineNumber);
        FileSource source = FileSource.from(filePath.toAbsolutePath().toFile(), position);
        return new JolkMethodTestDescriptor( methodDescriptorId, selectorName, source);
    }

    private String extractClassName(Path rootDir, Path filePath) {
        Path absRoot = rootDir.toAbsolutePath().normalize();
        Path absFile = filePath.toAbsolutePath().normalize();

        Path relativePath = absRoot.relativize(absFile);
        String pathString = relativePath.toString();

        if (pathString.endsWith(".jolk")) {
            pathString = pathString.substring(0, pathString.length() - ".jolk".length());
        }

        return pathString.replace('/', '.').replace('\\', '.');
    }

}