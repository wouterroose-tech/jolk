package jolk.test.engine;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JolkTestEngine_Test {

    JolkTestEngine testEngine = new JolkTestEngine();
    UniqueId baseId = UniqueId.forEngine("Jolk_Test");

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
        assertEquals(3, dirRoot.getChildren().size());
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
        assertEquals(3, apiContainer.getChildren().size()); // TestCase_Test, Test, TestCase
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
        context.load("jolk/test/api/TestSuite.jolk");
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
