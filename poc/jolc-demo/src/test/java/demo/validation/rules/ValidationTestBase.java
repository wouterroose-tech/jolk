package demo.validation.rules;

import org.graalvm.polyglot.Value;

import util.JolkTestBase;

public abstract class ValidationTestBase extends JolkTestBase {

    protected Value contactFormClass;
    Value personClass;
    Value validationSuiteClass;

    public void setUp() {
        super.setUp();
        // Load all necessary classes for the tests
        loadValidationEngine();
        contactFormClass = loadContactFormClass();
        personClass = loadPersonClass();
        validationSuiteClass = loadValidationSuite();
    }

    private void loadValidationEngine() {
        // load engine
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
    }

    private Value loadPersonClass() {
        return load("/demo/validation/domain/Person.jolk");
    }

    private Value loadContactFormClass() {
        return load("/demo/validation/domain/ContactForm.jolk");
    }

    private Value loadValidationSuite() {
        // load services
        load("/demo/validation/services/City.jolk");
        load("/demo/validation/services/GeoGraphicalService.jolk");
        // load rules
        load("/demo/validation/rules/SsnConstraint.jolk");
        load("/demo/validation/rules/ZipConstraint.jolk");
        return load("/demo/validation/rules/ContactFormValidation.jolk");
    }

}
