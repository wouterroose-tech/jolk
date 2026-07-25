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
        assertEquals(1, jolkEngineChildren.size());
        TestIdentifier testDirNode = jolkEngineChildren.iterator().next();
        assertEquals("test", testDirNode.getDisplayName());

        // Level 4: Get 'api' and 'engine' directory containers under 'test'
        Set<TestIdentifier> testDirChildren = testPlan.getChildren(testDirNode);
        assertEquals(2, testDirChildren.size(), "The 'test' directory must contain exactly 2 package folders");

        assertTrue(testDirChildren.stream().anyMatch(id -> id.getDisplayName().equals("api")));
        assertTrue(testDirChildren.stream().anyMatch(id -> id.getDisplayName().equals("engine")));
        TestIdentifier apiContainer = findIdentifierByDisplayName(testPlan, testDirChildren, "api");
        TestIdentifier engineContainer = findIdentifierByDisplayName(testPlan, testDirChildren, "engine");

        // Verify children of 'api' container (3 children)
        Set<TestIdentifier> apiChildren = testPlan.getChildren(apiContainer);
        assertEquals(2, apiChildren.size(), "The 'api' container must contain exactly 2 tests");
        assertTrue(containsDisplayName(apiChildren, "TestCase_Test"));
        assertTrue(containsDisplayName(apiChildren, "TestResult_Test"));

        // Verify children of 'engine' container (1 child)
        Set<TestIdentifier> engineChildren = testPlan.getChildren(engineContainer);
        assertEquals(1, engineChildren.size(), "The 'engine' container must contain exactly 1 test");
        assertTrue(containsDisplayName(engineChildren, "TestRunner_Test"));
    }

    private TestIdentifier findIdentifierByDisplayName(TestPlan testPlan, Set<TestIdentifier> identifiers, String expectedDisplayName) {
        return identifiers.stream()
            .filter(id -> id.getDisplayName().equals(expectedDisplayName))
            .findFirst()
            .orElseGet(() -> {
                // If not found in immediate set, search descendants recursively
                return identifiers.stream()
                        .flatMap(parent -> testPlan.getChildren(parent).stream())
                        .filter(id -> id.getDisplayName().equals(expectedDisplayName))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Expected container or test with display name '" + expectedDisplayName + "' not found"));
            });
    }
    
    private boolean containsDisplayName(Set<TestIdentifier> identifiers, String displayName) {
        return identifiers.stream()
                .anyMatch(id -> id.getDisplayName().equals(displayName));
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

        TestDescriptor jolkTestContainer = hierarchyRoot.getChildren().stream()
                .filter(d -> d.getDisplayName().equals("test"))
                .findFirst().orElseThrow();
        assertNotNull(jolkTestContainer);
        assertEquals(2, jolkTestContainer.getChildren().size()); // api and engine

        TestDescriptor apiContainer = jolkTestContainer.getChildren().stream()
                .filter(d -> d.getDisplayName().equals("api"))
                .findFirst().orElseThrow();
        assertNotNull(apiContainer);
        assertEquals(2, apiContainer.getChildren().size()); // TestCase_Test, Test, TestCase
        assertTrue(apiContainer.getChildren().stream()
                .anyMatch(d -> d.getDisplayName().equals("TestCase_Test")));

        TestDescriptor engineContainer = jolkTestContainer.getChildren().stream()
                .filter(d -> d.getDisplayName().equals("engine"))
                .findFirst().orElseThrow();
        assertNotNull(engineContainer);
        assertEquals(1, engineContainer.getChildren().size()); // TestRunner_Test
        assertTrue(engineContainer.getChildren().stream()
                .anyMatch(d -> d.getDisplayName().equals("TestRunner_Test")));
    }

    /// Test unit test discovery classpath root directory hierarchy selection
    @Test
    void testDiscoverWithClasspathSelector() {
        //TODO
    }

    @Test
    void testFolderDescriptor() {
        JolkTestRuntimeContext context = new JolkTestRuntimeContext();
        //load superclasses;
        context.load("jolk/test/api/Test.jolk");
        context.load("jolk/test/api/TestCase.jolk");
        UniqueId baseId = UniqueId.forEngine("TestEngine");
        JolkEngineDescriptor rootDescriptor = new JolkEngineDescriptor(baseId, context);
        TestDescriptor folderDescriptor = testEngine.folderDescriptor(rootDescriptor, "jolk\\test\\api");
        assertEquals("[engine:TestEngine]/[directory:jolk\\test\\api]", folderDescriptor.getUniqueId().toString());
    }

    @Test
    void testClassDescriptor() {
        JolkTestRuntimeContext context = new JolkTestRuntimeContext();
        //load superclasses;
        context.load("jolk/test/api/Test.jolk");
        context.load("jolk/test/api/TestCase.jolk");
        UniqueId baseId = UniqueId.forEngine("TestEngine");
        UniqueId folderId = baseId.append("directory", "jolk\\test\\api");
        Path path = Path.of("/jolk/test/api/TestCase_Test.jolk");
        JolkClassTestDescriptor testDescriptor = testEngine.classDescriptor(context, folderId, path);
        assertEquals("TestCase_Test", testDescriptor.getDisplayName());
        assertEquals("[engine:TestEngine]/[directory:jolk\\test\\api]/[class:TestCase_Test]", testDescriptor.getUniqueId().toString());
        assertEquals(11, testDescriptor.getChildren().size());
    }

    @Test
    void testMethodDescriptor() {
        JolkTestEngine testEngine = new JolkTestEngine();
        UniqueId baseId = UniqueId.forEngine("TestEngine");
        UniqueId classId = baseId.append("class", "TestClass");
        JolkMethodTestDescriptor testDescriptor = testEngine.methodDescriptor(classId, "testCase");
        assertEquals("testCase", testDescriptor.getDisplayName());
        assertEquals("[engine:TestEngine]/[class:TestClass]/[method:testCase]", testDescriptor.getUniqueId().toString());
    }

}
