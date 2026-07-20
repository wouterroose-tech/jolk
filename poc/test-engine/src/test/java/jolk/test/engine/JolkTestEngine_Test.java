package jolk.test.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

public class JolkTestEngine_Test {

    private class DiscoveryRequest implements EngineDiscoveryRequest {

        @Override
        public ConfigurationParameters getConfigurationParameters() {
            return null;
        }

        @Override
        public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> filterType) {
            return List.of();
        }

        @Override
        public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> selectorType) {
            return List.of();
        }

    } 

    @Test
    void testDiscover() {
        JolkTestEngine testEngine = new JolkTestEngine();
        UniqueId baseId = UniqueId.forEngine("TestEngine");
        // create discoveryRequest for src/test/jolk/test/api/
        TestDescriptor rootDescriptor = testEngine.discover(new DiscoveryRequest(), baseId);
        assertNotNull(rootDescriptor);
        //verify rootDescriptor contains class descriptors for each class in the directory

        // create discoveryRequest for src/test/jolk/
        rootDescriptor = testEngine.discover(new DiscoveryRequest(), baseId);
        assertNotNull(rootDescriptor);
        //verify rootDescriptor hierarchy correcsponds the directory hierarchy
    }

    @Test
    void testClassDescriptor() {
        JolkTestEngine testEngine = new JolkTestEngine();
        JolkTestRuntimeContext context = new JolkTestRuntimeContext();
        //load superclasses;
        context.load("jolk/test/api/Test.jolk");
        context.load("jolk/test/api/TestCase.jolk");
        UniqueId baseId = UniqueId.forEngine("TestEngine");
        Path path = Path.of("/jolk/test/api/TestCase_Test.jolk");
        JolkClassTestDescriptor testDescriptor = testEngine.classDescriptor(context, baseId, path);
        assertEquals("TestCase_Test", testDescriptor.getDisplayName());
        assertEquals("[engine:TestEngine]/[directory:\\jolk\\test\\api]/[class:TestCase_Test]", testDescriptor.getUniqueId().toString());
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
