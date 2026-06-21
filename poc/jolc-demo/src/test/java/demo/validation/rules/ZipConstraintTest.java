package demo.validation.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ZipConstraintTest extends ValidationTestBase {

    @BeforeEach
    public void setUp() {
        super.setUp();
        setUp("/demo/validation/rules/ZipConstraintTest.jolk");
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
    public void test_isMalinesArea_success() {
        test("test_isMalinesArea_success");
    }

    @Test
    public void test_isMalinesArea_failure() {
        test("test_isMalinesArea_failure");
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
    public void test_getIssue() {
        test("test_getIssue");
    }

}