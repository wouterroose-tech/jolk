package jolk.test.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import tolk.runtime.JolkMetaClass;

public class JolkTestRuntimeContext_Test {

    @Test
    void testLoadTestFramework() {
        JolkTestRuntimeContext runtimeContext = new JolkTestRuntimeContext();
        runtimeContext.loadDirectory("jolk/test/api");
        // TODO change to lookup a laded class
        JolkMetaClass metaClass = runtimeContext.getDefinedClass(runtimeContext.evaluateJolkSource(Path.of("/jolk/test/api/TestCase.jolk")));
        assertNotNull(metaClass);
        assertEquals("jolk.test.api.TestCase", metaClass.name);
    }

    @Test
    void testEvaluateJolkSource() {
        JolkTestRuntimeContext runtimeContext = new JolkTestRuntimeContext();
        runtimeContext.loadDirectory("jolk/test/api");
        JolkMetaClass metaClass = runtimeContext.getDefinedClass(runtimeContext.evaluateJolkSource(Path.of("/jolk/test/api/TestCase_Test.jolk")));
        assertNotNull(metaClass);
        assertEquals("jolk.test.api.TestCase_Test", metaClass.name);
    }

    @Test
    void testExtractTestSelectors() {
        JolkTestRuntimeContext runtimeContext = new  JolkTestRuntimeContext();
        runtimeContext.loadDirectory("jolk/test/api");
        JolkMetaClass metaClass = runtimeContext.getDefinedClass(runtimeContext.evaluateJolkSource(Path.of("/jolk/test/api/TestCase_Test.jolk")));

        // Extract test selectors from the TestCase class
        var selectors = runtimeContext.getTestSelectors(metaClass).toList();
        assertNotNull(selectors);
        assertEquals(12, selectors.size());
        // check if selectors contain the expected test method name "testSuccess"
        assertNotNull(selectors.contains("testSuccess"));
    }

}
