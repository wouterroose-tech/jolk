package demo.validation.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ZipConstraintTest extends ValidationTestBase {


    Value testInstance;

    @BeforeEach
    public void setUp() {
        super.setUp();
        // Create the Jolk test instance
        Value testclass = getJolkClass("/examples/ZipConstraintTest.jolk");
        testInstance = testclass.invokeMember("new");
    }
    @Test
    public void test_ZipConstraint_success() {
        assertTrue(testInstance.invokeMember("test_ZipConstraint_success").asBoolean());
    }

    @Test
    public void test_ZipConstraint_failure() {
        assertTrue(testInstance.invokeMember("test_ZipConstraint_failure").asBoolean());
    }

}