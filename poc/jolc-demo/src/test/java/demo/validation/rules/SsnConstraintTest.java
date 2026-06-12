package demo.validation.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SsnConstraintTest  extends ValidationTestBase {

    @BeforeEach
    public void setUp() {
        setUp("/demo/validation/rules/SsnConstraintTest.jolk");
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
    public void test_isValid_success() {
        assertTrue(testInstance.invokeMember("test_isValid_success").asBoolean());
    }

    @Test
    public void test_isValid_failure() {
        assertTrue(testInstance.invokeMember("test_isValid_failure").asBoolean());
    }

    @Test
    public void test_isValid_logic() {
        assertTrue(testInstance.invokeMember("test_isValid_logic").asBoolean());
    }

    @Test
    public void test_getIssue() {
        assertTrue(testInstance.invokeMember("test_getIssue").asBoolean());
    }

    @Test
    public void test_interrupt() {
        assertTrue(testInstance.invokeMember("test_interrupt").asBoolean());
    }

}
