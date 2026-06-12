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
        Value testclass = getJolkClass("/demo/validation/rules/ZipConstraintTest.jolk");
        testInstance = testclass.invokeMember("new");
    }
    
    @Test
    public void test_accept_success() {
        assertTrue(testInstance.invokeMember("test_accept_success").asBoolean());
    }

    @Test
    public void test_accept_failure() {
        assertTrue(testInstance.invokeMember("test_accept_failure").asBoolean());
    }

    @Test
    public void test_satisfiesPreCondition_success() {
        assertTrue(testInstance.invokeMember("test_satisfiesPreCondition_success").asBoolean());
    }

    @Test
    public void test_satisfiesPreCondition_failure() {
        assertTrue(testInstance.invokeMember("test_satisfiesPreCondition_failure").asBoolean());
    }

    @Test
    public void test_isMalinesArea_success() {
        assertTrue(testInstance.invokeMember("test_isMalinesArea_success").asBoolean());
    }

    @Test
    public void test_isMalinesArea_failure() {
        assertTrue(testInstance.invokeMember("test_isMalinesArea_failure").asBoolean());
    }

    @Test
    public void test_isValid_success() {
        assertTrue(testInstance.invokeMember("test_isValid_success").asBoolean());
    }

    @Test
    public void test_isValid_failure() {
        assertTrue(testInstance.invokeMember("test_isValid_failure").asBoolean());
    }

    @Test
    public void test_getIssue() {
        assertTrue(testInstance.invokeMember("test_getIssue").asBoolean());
    }

}