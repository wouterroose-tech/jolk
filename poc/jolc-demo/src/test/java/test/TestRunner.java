package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.graalvm.polyglot.Value;

import util.JolkTestBase;


/// # TestRunner
///
/// run the jolk TestRunner, will be replaced by ...
///
/// @author Wouter Roose
///
public class TestRunner extends JolkTestBase {
    
    protected Value test;

    @BeforeEach
    public void setUp() {
        super.setUp();
        super.load("/test/api/TestCase_Test.jolk");
        super.load("/test/api/TestSuite_Test.jolk");
        super.load("/test/api/TestResult_Test.jolk");
        
        
        super.load("demo/validation/domain/Person.jolk");
        super.load("demo/validation/domain/PersonTest.jolk");
        super.load("demo/validation/domain/ContactForm.jolk");
        super.load("demo/validation/domain/ContactFormTest.jolk");

        super.load("/examples/Circle.jolk");
        super.load("/examples/CircleTest.jolk");

        super.setUp("/test/engine/TestRunner.jolk");
    }
    
    @Test
    public void testRun() {
        testInstance.invokeMember("run");
    }

}
