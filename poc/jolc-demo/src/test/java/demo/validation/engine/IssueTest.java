package demo.validation.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import demo.validation.rules.ValidationTestBase;


public class IssueTest extends ValidationTestBase {

    @BeforeEach
    public void setUp() {
        super.setUp();
        super.setUp("/demo/validation/engine/IssueTest.jolk");
    }

    @Test
    public void test_getIssue() {
        test("test_getIssue");
    }

}
