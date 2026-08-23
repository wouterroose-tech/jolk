package jolk.test;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/// # TestFramework_Test
///
/// run the jolk tests for the Jolk test framework
///
/// @author Wouter Roose
///
// Disabled, test now run via the JolkTestEngine for the JUnit test framework
@Disabled
public class TestFramework_Test extends TestRunner  {
    
    @Test
    public void testTestCase_Test() {
        Value testClass = load("/jolk/test/api/TestCase_Test.jolk");
        runTestClass(testClass);
    }
    
    @Test
    public void testTestResult_Test() {
        Value testClass = load("/jolk/test/api/TestResult_Test.jolk");
        runTestClass(testClass);
    }
    
    @Test
    public void testTestSuite_Test() {
        Value testClass = load("/jolk/test/api/TestSuite_Test.jolk");
        runTestClass(testClass);
    }
    
    @Test
    public void testTestRunner_Test() {
        Value testClass = load("/jolk/test/engine/TestRunner_Test.jolk");
        runTestClass(testClass);
    }
    
    @Test
    public void testTestApiTestRunner() {
        load("/jolk/test/api/TestCase_Test.jolk");
        load("/jolk/test/api/TestResult_Test.jolk");
        load("/jolk/test/api/TestSuite_Test.jolk");
        load("/jolk/test/engine/TestRunner_Test.jolk");
        Value testClass = load("/jolk/test/TestApiTestRunner.jolk");
        run(testClass);
    }

}
