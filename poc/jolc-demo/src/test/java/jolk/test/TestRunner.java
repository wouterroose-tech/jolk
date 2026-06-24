package jolk.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.graalvm.polyglot.Value;

import util.JolkTestBase;

public class TestRunner extends JolkTestBase {
    
    protected Value test;

    @BeforeEach
    public void setUp() {
        super.setUp();
        super.load("/test/api/TestCase_Test.jolk");
        super.load("/test/api/TestSuite_Test.jolk");
        super.load("/test/api/TestResult_Test.jolk");
        super.setUp("/test/TestRunner.jolk");
    }
    
    @Test
    public void testRun() {
        testInstance.invokeMember("run");
    }

}
