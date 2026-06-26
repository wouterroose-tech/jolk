package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.graalvm.polyglot.Value;

import util.JolkTestBase;


/// # TestRunner
///
/// run the jolk TestRunner, will be replaced by the JolkTestEngine for the JUnit test framework
///
/// @author Wouter Roose
///
public class TestRunner extends JolkTestBase {
    
    protected Value test;

    @BeforeEach
    public void setUp() {
        super.setUp();

        // test framework
        load("/test/api/Test.jolk");
        load("/test/api/TestCase.jolk");
        load("/test/api/TestSuite.jolk");
        load("/test/api/TestResult.jolk");
        load("/test/api/TestStatus.jolk");
        load("/test/api/AssertionSignal.jolk");
        load("/test/api/DisabledSignal.jolk");
        load("/test/api/TimeoutSignal.jolk");
        load("/test/api/TestCase_Test.jolk");
        load("/test/api/TestSuite_Test.jolk");
        load("/test/api/TestResult_Test.jolk");
        // examples
        load("/examples/Circle.jolk");
        load("/examples/CircleTest.jolk");
        // demonstrators
        load("/demonstrators/ArchetypeClassDemonstrator.jolk");
        load("/demonstrators/ArchetypeClassDemonstratorTest.jolk");
        // domain
        load("/demo/validation/domain/Person.jolk");
        load("/demo/validation/domain/PersonTest.jolk");
        load("/demo/validation/domain/ContactForm.jolk");
        load("/demo/validation/domain/ContactFormTest.jolk");
        // validation engine
        load("/demo/validation/engine/Level.jolk");
        load("/demo/validation/engine/Issue.jolk");
        load("/demo/validation/engine/IssueTest.jolk");
        load("/demo/validation/engine/Interrupt.jolk");
        load("/demo/validation/engine/ExecutionContext.jolk");
        load("/demo/validation/engine/Node.jolk");
        load("/demo/validation/engine/ChildValidation.jolk");
        load("/demo/validation/engine/ChildrenValidation.jolk");
        load("/demo/validation/engine/Validation.jolk");
        load("/demo/validation/engine/Constraint.jolk");
        load("/demo/validation/engine/ValidationSuite.jolk");
        // business services
        load("/demo/validation/services/City.jolk");
        load("/demo/validation/services/GeoGraphicalService.jolk");
        // business validation rules
        load("/demo/validation/rules/SsnConstraint.jolk");
        load("/demo/validation/rules/SsnConstraintTest.jolk");
        load("/demo/validation/rules/ZipConstraint.jolk");
        load("/demo/validation/rules/ZipConstraintTest.jolk");
        load("/demo/validation/rules/ContactFormValidation.jolk");
        load("/demo/validation/rules/ContactFormValidationTest.jolk");

        setUp("/test/engine/TestRunner.jolk");
    }
    
    @Test
    public void testRun() {
        testInstance.invokeMember("run");
    }

}
