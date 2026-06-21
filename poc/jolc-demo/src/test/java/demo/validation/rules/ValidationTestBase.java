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
        getJolkClass("/demo/validation/engine/Level.jolk");
        getJolkClass("/demo/validation/engine/Issue.jolk");
        getJolkClass("/demo/validation/engine/Interrupt.jolk");
        getJolkClass("/demo/validation/engine/ExecutionContext.jolk");
        getJolkClass("/demo/validation/engine/Node.jolk");
        getJolkClass("/demo/validation/engine/ChildValidation.jolk");
        getJolkClass("/demo/validation/engine/ChildrenValidation.jolk");
        getJolkClass("/demo/validation/engine/Validation.jolk");
        getJolkClass("/demo/validation/engine/Constraint.jolk");
        getJolkClass("/demo/validation/engine/ValidationSuite.jolk");
    }

    private Value loadPersonClass() {
        return getJolkClass("/demo/validation/domain/Person.jolk");
    }

    private Value loadContactFormClass() {
        return getJolkClass("/demo/validation/domain/ContactForm.jolk");
    }

    private Value loadValidationSuite() {
        // load services
        getJolkClass("/demo/validation/services/City.jolk");
        getJolkClass("/demo/validation/services/GeoGraphicalService.jolk");
        // load rules
        getJolkClass("/demo/validation/rules/SsnConstraint.jolk");
        getJolkClass("/demo/validation/rules/ZipConstraint.jolk");
        return getJolkClass("/demo/validation/rules/ContactFormValidation.jolk");
    }

}
