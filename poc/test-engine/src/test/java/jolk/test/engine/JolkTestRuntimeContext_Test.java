package jolk.test.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import tolk.runtime.JolkMetaClass;

public class JolkTestRuntimeContext_Test {

    @Test
    public void testLoadTestFramework() {
        JolkTestRuntimeContext runtimeContext = new JolkTestRuntimeContext();
        runtimeContext.loadDirectory("jolk/test/api");
        JolkMetaClass metaClass = runtimeContext.getDefinedClass("jolk.test.api.TestCase");
        assertNotNull(metaClass);
        assertEquals("jolk.test.api.TestCase", metaClass.name);
    }

    @Test
    public void testEvaluateJolkSource() {
        JolkTestRuntimeContext runtimeContext = new JolkTestRuntimeContext();
        runtimeContext.loadDirectory("jolk/test/api");
        JolkMetaClass metaClass = runtimeContext.evaluateJolkSource(Path.of("/jolk/test/api/TestCase_Test.jolk"));
        assertNotNull(metaClass);
        assertEquals("jolk.test.api.TestCase_Test", metaClass.name);
        assertEquals(11, runtimeContext.getTestSelectors(metaClass).count());
    }

    @Test
    void testExtractTestSelectors() {
        JolkTestRuntimeContext runtimeContext = new  JolkTestRuntimeContext();
        runtimeContext.loadDirectory("jolk/test/api");
        runtimeContext.load("/jolk/test/api/TestCase_test.jolk");
        JolkMetaClass metaClass = runtimeContext.getDefinedClass("jolk.test.api.TestCase_Test");
        assertNotNull(metaClass);
        assertEquals("jolk.test.api.TestCase_Test", metaClass.name);

        // Extract test selectors from the TestCase class
        var selectors = runtimeContext.getTestSelectors(metaClass).toList();
        assertNotNull(selectors);
        assertEquals(11, selectors.size());
        // check if selectors contain the expected test method name "testSuccess"
        assertNotNull(selectors.contains("testSuccess"));
    }

}
