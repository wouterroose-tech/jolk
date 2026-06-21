package demo.validation.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SsnConstraintTest extends ValidationTestBase {

    @BeforeEach
    public void setUp() {
        super.setUp();
        setUp("/demo/validation/rules/SsnConstraintTest.jolk");
    }
    
    @Test
    public void test_accept_success() {
        test("test_accept_success");
    }

    @Test
    public void test_accept_failure() {
        test("test_accept_failure");
    }

    @Test
    public void test_satisfiesPreCondition_success() {
        test("test_satisfiesPreCondition_success");
    }

    @Test
    public void test_satisfiesPreCondition_failure() {
        test("test_satisfiesPreCondition_failure");
    }

    @Test
    public void test_isValid_success() {
        test("test_isValid_success");
    }

    @Test
    public void test_isValid_failure() {
        test("test_isValid_failure");
    }

    @Test
    public void test_isValid_logic() {
        test("test_isValid_logic");
    }

    @Test
    public void test_getIssue() {
        test("test_getIssue");
    }

    @Test
    public void test_interrupt() {
        test("test_interrupt");
    }

}
