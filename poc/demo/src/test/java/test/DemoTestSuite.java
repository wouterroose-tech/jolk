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
        // examples
        load("/examples/Circle.jolk");
        load("/examples/Complex.jolk");
        // domain
        load("/demo/validation/domain/ContactForm.jolk");
        load("/demo/validation/domain/Person.jolk");
        load("/demo/validation/domain/User.jolk");
        // validation engine
        load("/demo/validation/engine/Level.jolk");
        load("/demo/validation/engine/Issue.jolk");
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
        load("/demo/validation/services/UserManager.jolk");
        load("/demo/validation/services/UserRepository.jolk");
        // mock
        load("/mock/UserRepositoryMock.jolk");
        // demonstrators
        load("/demonstrators/CollectionLiteralDemonstrator.jolk");
        load("/demonstrators/CoreCollectionDemonstrator.jolk");
        // validation rules
        load("/demo/validation/rules/SsnConstraint.jolk");
        load("/demo/validation/rules/ZipConstraint.jolk");
        load("/demo/validation/rules/ContactFormValidation.jolk");
    }
    
    @Test
    @Disabled
    public void runAllTests() {
        load("/examples/CircleTest.jolk");
        load("/examples/ComplexTest.jolk");
        load("/demo/validation/domain/PersonTest.jolk");
        load("/demo/validation/domain/ContactFormTest.jolk");
        load("/demonstrators/ArchetypeClassTest.jolk");
        load("/demonstrators/ArchetypeEnumTest.jolk");
        load("/demonstrators/ArchetypeRecordTest.jolk");
        load("/demonstrators/ClosureTest.jolk");
        load("/demonstrators/CollectionLiteralTest.jolk");
        load("/demonstrators/CoreCollectionTest.jolk");
        load("/demonstrators/CoreProtocolTest.jolk");
        load("/demonstrators/EqualityTest.jolk");
        load("/demonstrators/ExceptionHandlingTest.jolk");
        load("/demo/validation/engine/LevelTest.jolk");
        load("/demo/validation/engine/IssueTest.jolk");
        load("/demo/validation/rules/SsnConstraint.jolk");
        load("/demo/validation/rules/SsnConstraintTest.jolk");
        load("/demo/validation/rules/ZipConstraintTest.jolk");
        load("/demo/validation/rules/ContactFormValidationTest.jolk");
        load("/mock/MockTest.jolk");
        load("/DemoTestRunner.jolk")
            .invokeMember("new")
            .invokeMember("run");
    }
    
    @Test
    public void runDemonstratorTest() {
        
        Value testClass;

        testClass = load("/demonstrators/ClassTest.jolk");
        runTestClass(testClass);
        
        testClass = load("/demonstrators/ClosureTest.jolk");
        runTestClass(testClass);

        testClass = load("/demonstrators/CollectionLiteralTest.jolk");
        runTestClass(testClass);

        testClass = load("/demonstrators/CoreCollectionTest.jolk");
        runTestClass(testClass);
        
        testClass = load("/demonstrators/EnumTest.jolk");
        runTestClass(testClass);
        
        testClass = load("/demonstrators/EqualityTest.jolk");
        runTestClass(testClass);

        testClass = load("/demonstrators/ExceptionHandlingTest.jolk");
        runTestClass(testClass);
        
        testClass = load("/demonstrators/ExpressionTest.jolk");
        runTestClass(testClass);

        load("/demonstrators/StableAndConstantFieldTest.jolk");
        runTestClass(testClass);

        testClass = load("/demonstrators/PresenceTest.jolk");
        runTestClass(testClass);
        
        testClass = load("/demonstrators/RecordTest.jolk");
        runTestClass(testClass);

        testClass = load("/demonstrators/TernaryExpressionTest.jolk");
        runTestClass(testClass);
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
    
    @Test
    public void runMockTest() {
        Value testClass;
        testClass = load("/mock/MockTest.jolk");
        runTestClass(testClass);
    }

}