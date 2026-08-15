package test;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jolk.test.TestRunner;

/// # TestRunner
///
/// run the jolk TestRunner, will be replaced by the JolkTestEngine for the JUnit test framework
///
/// @author Wouter Roose
///
public class DemoTestSuite extends TestRunner {

    @BeforeAll
    public static void setUp() {
        jolk.test.TestRunner.setUp();
        
        // domain
        load("/demo/validation/domain/Person.jolk");
        load("/demo/validation/domain/ContactForm.jolk");

        // validation engine
        load("/demo/validation/engine/Level.jolk");
        load("/demo/validation/engine/LevelTest.jolk");
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
    }
    
    @Test
    @Disabled
    public void runAllTests() {
        load("/DemoTestRunner.jolk")
            .invokeMember("new")
            .invokeMember("run");
    }
    
    @Test
    public void runDemonstratorTest() {
        Value testClass;
        load("/demonstrators/CollectionLiteralDemonstrator.jolk");
        testClass = load("/demonstrators/CollectionLiteralDemonstratorTest.jolk");
        runTestClass(testClass);

        load("/demonstrators/CoreCollectionDemonstrator.jolk");
        testClass = load("/demonstrators/CoreCollectionDemonstratorTest.jolk");
        runTestClass(testClass);
        
        testClass = load("/demonstrators/ArchetypeClassDemonstratorTest.jolk");
        runTestClass(testClass);
        
        testClass = load("/demonstrators/ArchetypeEnumDemonstratorTest.jolk");
        runTestClass(testClass);
        
        testClass = load("/demonstrators/ArchetypeRecordDemonstratorTest.jolk");
        runTestClass(testClass);
        
        testClass = load("/demonstrators/ClosureDemonstratorTest.jolk");
        runTestClass(testClass);
        
        testClass = load("/demonstrators/EqualityDemonstratorTest.jolk");
        runTestClass(testClass);

        testClass = load("/demonstrators/ExceptionHandlingDemonstratorTest.jolk");
        runTestClass(testClass);

        load("/demonstrators/StableAndConstantFieldDemonstrator.jolk");
    }
    
    // examples
    @Test
    public void runCircleTest() {
        load("/examples/Circle.jolk");
        Value testClass = load("/examples/CircleTest.jolk");
        runTestClass(testClass);
    }
    
    @Test
    public void runComplexTest() {
        load("/examples/Complex.jolk");
        Value testClass = load("/examples/ComplexTest.jolk");
        runTestClass(testClass);
    }
    
    @Test
    public void runDomainTest() {
        Value personTest = load("/demo/validation/domain/PersonTest.jolk");
        runTestClass(personTest);

        Value contactFormTest = load("/demo/validation/domain/ContactFormTest.jolk");
        runTestClass(contactFormTest);
    }
    
    @Test
    public void runValidationTest() {
        Value testClass;
        load("/demo/validation/rules/SsnConstraint.jolk");
        testClass = load("/demo/validation/rules/SsnConstraintTest.jolk");
        runTestClass(testClass);

        load("/demo/validation/rules/ZipConstraint.jolk");
        testClass = load("/demo/validation/rules/ZipConstraintTest.jolk");
        runTestClass(testClass);

        load("/demo/validation/rules/ContactFormValidation.jolk");
        testClass = load("/demo/validation/rules/ContactFormValidationTest.jolk");
        runTestClass(testClass);
    }
    
    @Test
    public void runValidationEngineTest() {
        Value testClass;
        testClass = load("/demo/validation/engine/LevelTest.jolk");
        runTestClass(testClass);
        
        testClass = load("/demo/validation/engine/IssueTest.jolk");
        runTestClass(testClass);
    }

}