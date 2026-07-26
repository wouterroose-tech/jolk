package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// # TestRunner
///
/// run the jolk TestRunner, will be replaced by the JolkTestEngine for the JUnit test framework
///
/// @author Wouter Roose
///
public class TestRunner extends jolk.test.TestRunner {

    @BeforeEach
    public void setUp() {
        super.setUp();

        // test framework
        // examples
        load("/examples/Circle.jolk");
        load("/examples/CircleTest.jolk");
        load("/examples/Complex.jolk");
        load("/examples/ComplexTest.jolk");
        // demonstrators
        load("/demonstrators/ArchetypeClassDemonstrator.jolk");
        load("/demonstrators/ArchetypeClassDemonstratorTest.jolk");
        load("/demonstrators/ArchetypeEnumDemonstrator.jolk");
        load("/demonstrators/ArchetypeEnumDemonstratorTest.jolk");
        load("/demonstrators/ArchetypeRecordDemonstrator.jolk");
        load("/demonstrators/ArchetypeRecordDemonstratorTest.jolk");
        load("/demonstrators/ClosureDemonstrator.jolk");
        load("/demonstrators/ClosureDemonstratorTest.jolk");
        load("/demonstrators/CollectionLiteralDemonstrator.jolk");
        load("/demonstrators/CollectionLiteralDemonstratorTest.jolk");
        load("/demonstrators/CoreCollectionDemonstrator.jolk");
        load("/demonstrators/CoreCollectionDemonstratorTest.jolk");
        
        // domain
        load("/demo/validation/domain/Person.jolk");
        load("/demo/validation/domain/PersonTest.jolk");
        load("/demo/validation/domain/ContactForm.jolk");
        load("/demo/validation/domain/ContactFormTest.jolk");
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
        // business validation rules
        load("/demo/validation/rules/SsnConstraint.jolk");
        load("/demo/validation/rules/SsnConstraintTest.jolk");
        load("/demo/validation/rules/ZipConstraint.jolk");
        load("/demo/validation/rules/ZipConstraintTest.jolk");
        load("/demo/validation/rules/ContactFormValidation.jolk");
        load("/demo/validation/rules/ContactFormValidationTest.jolk");
    }
    
    @Test
    public void testRun() {
        load("/DemoTestRunner.jolk")
            .invokeMember("new")
            .invokeMember("run");
    }

}
