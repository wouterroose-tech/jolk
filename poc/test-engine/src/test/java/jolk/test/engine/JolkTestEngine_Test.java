package jolk.test.engine;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import jolk.test.JolkTestSuite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JolkTestEngine_Test {

    JolkTestEngine testEngine = new JolkTestEngine();
    UniqueId baseId = UniqueId.forEngine("Jolk_Test");

    @Test
    void testDiscoverViaSuiteEngine() {
        LauncherDiscoveryRequest suiteRequest = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(DiscoverySelectors.selectClass(JolkTestSuite.class))
                .build();
        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(suiteRequest);

        assertNotNull(testPlan);
        assertTrue(testPlan.containsTests(), "TestPlan generated via JolkTestSuite must contain test nodes");
        boolean containsJolkEngine = testPlan.getRoots().stream()
                .anyMatch(root -> root.getUniqueId().contains("engine:jolk"));
        assertTrue(containsJolkEngine, "TestPlan should contain nodes discovered by JolkTestEngine");
 
        // Locate the Jolk engine root identifier
        TestIdentifier suiteEngineRoot = testPlan.getRoots().stream()
                .filter(root -> root.getUniqueId().equals("[engine:junit-platform-suite]"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Suite engine root missing from TestPlan"));

        // Level 1: Get 'JolkTestSuite' node under Suite engine
        Set<TestIdentifier> suiteEngineChildren = testPlan.getChildren(suiteEngineRoot);
        assertEquals(1, suiteEngineChildren.size());
        TestIdentifier suiteAnchor = suiteEngineChildren.iterator().next();
        assertEquals("JolkTestSuite", suiteAnchor.getDisplayName());

        // Level 2: Get 'Jolk Test Engine' node under 'JolkTestSuite'
        Set<TestIdentifier> suiteAnchorChildren = testPlan.getChildren(suiteAnchor);
        assertEquals(1, suiteAnchorChildren.size());
        TestIdentifier jolkEngineNode = suiteAnchorChildren.iterator().next();
        assertEquals("Jolk Test Engine", jolkEngineNode.getDisplayName());

        // Level 3: Get 'test' directory node under 'Jolk Test Engine'
        Set<TestIdentifier> jolkEngineChildren = testPlan.getChildren(jolkEngineNode);
        assertTrue(
                jolkEngineChildren.stream().noneMatch(id -> id.getDisplayName().equals("test")),
                "Directory container 'test' must not exist in a flattened descriptor tree"
                );

        // Verify immediate class children under the engine node
        // should contain 3 classes
        assertEquals(3, jolkEngineChildren.size());
        boolean hasApiClass = jolkEngineChildren.stream()
                .anyMatch(id -> id.getLegacyReportingName().equals("TestCase_Test"));
        assertTrue(hasApiClass, "Engine root must contain 'jolk.test.api.TestCase_Test' directly");
        boolean hasEngineClass = jolkEngineChildren.stream()
                .anyMatch(id -> id.getLegacyReportingName().equals("TestRunner_Test"));
        assertTrue(hasEngineClass, "Engine root must contain engine test classes directly");
    }

    /// Test unit test discovery for single file selection
    @Test
    void testDiscoverWithFileSelector() {
        EngineDiscoveryRequest fileRequest = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(DiscoverySelectors.selectFile("src/test/jolk/test/api/TestCase_Test.jolk"))
                .build();
        TestDescriptor fileRoot = testEngine.discover(fileRequest, baseId);
        assertNotNull(fileRoot);
        assertFalse(fileRoot.getChildren().isEmpty());
        assertTrue(fileRoot.getChildren().stream()
                .anyMatch(child -> child.getDisplayName().equals("TestCase_Test")));        
    }

    /// Test unit test discovery for directory selection (Scans subdirectory hierarchy)
    @Test
    void testDiscoverWithDirectorySelector() {
        EngineDiscoveryRequest dirRequest = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(DiscoverySelectors.selectDirectory("src/test/jolk/test/api"))
                .build();
        TestDescriptor dirRoot = testEngine.discover(dirRequest, baseId);
        assertNotNull(dirRoot);
        // Verify children reflect discovered Jolk descriptors
        assertEquals(2, dirRoot.getChildren().size());
    }

    
    /// Test unit test discovery root directory hierarchy selection
    @Test
    void testDiscoverWithRootSelector() {
        EngineDiscoveryRequest rootDirRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectDirectory("src/test/jolk"))
                .build();

        TestDescriptor hierarchyRoot = testEngine.discover(rootDirRequest, baseId);
        assertNotNull(hierarchyRoot);
        // Verify structural nodes exist in the child tree
        assertTrue(hierarchyRoot.getChildren().stream()
                .anyMatch(TestDescriptor::isContainer));

        // 3. Find the target class descriptor directly via its FQCN display name or ID
        Set<? extends TestDescriptor> children = hierarchyRoot.getChildren();
        assertEquals(3, children.size());
        TestDescriptor testCaseClassDescriptor = children.stream()
                .filter(d -> d.getDisplayName().contains("TestCase_Test"))
                .findFirst()
                .orElse(null);
        assertNotNull(testCaseClassDescriptor, "Class descriptor for 'TestCase_Test' not found");
        testCaseClassDescriptor = children.stream()
                .filter(d -> d.getDisplayName().contains("TestRunner_Test"))
                .findFirst()
                .orElse(null);
        assertNotNull(testCaseClassDescriptor, "Class descriptor for 'TestRunner_Test' not found");
    }

    /// Test unit test discovery classpath root directory hierarchy selection
    @Test
    void testDiscoverWithClasspathSelector() {
        //TODO
    }

    @Test
    void testClassDescriptor() {
        JolkTestRuntimeContext context = new JolkTestRuntimeContext();
        //load superclasses;
        context.load("jolk/test/api/Test.jolk");
        context.load("jolk/test/api/TestCase.jolk");
        UniqueId baseId = UniqueId.forEngine("TestEngine");
        UniqueId folderId = baseId.append("directory", "jolk.test.api");
        Path rootDir = Path.of("/jolk/");
        Path path = Path.of("/jolk/test/api/TestCase_Test.jolk");
        JolkClassTestDescriptor testDescriptor = testEngine.classDescriptor(context, folderId, rootDir, path);
        assertEquals("TestCase_Test", testDescriptor.getDisplayName());
        UniqueId expectedId = folderId.append("class", "test.api.TestCase_Test");
        assertEquals(expectedId, testDescriptor.getUniqueId());
        assertEquals(12, testDescriptor.getChildren().size());
    }

    @Test
    void testMethodDescriptor() {
        JolkTestEngine testEngine = new JolkTestEngine();
        UniqueId baseId = UniqueId.forEngine("TestEngine");
        UniqueId classId = baseId.append("class", "TestClass");
        Path path = Path.of("src/test/jolk/test/api/TestClass.jolk");
        JolkMethodTestDescriptor testDescriptor = testEngine.methodDescriptor(classId, "testCase", path, 10);
        assertEquals("testCase", testDescriptor.getDisplayName());
        assertEquals("[engine:TestEngine]/[class:TestClass]/[test:testCase]", testDescriptor.getUniqueId().toString());
    }

}
