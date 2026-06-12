package demo.validation.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import demo.validation.rules.ValidationTestBase;


public class IssueTest extends ValidationTestBase {

    @BeforeEach
    public void setUp() {
        super.setUp("/demo/validation/engine/IssueTest.jolk");
    }

    @Test
    public void test_getIssue() {
        assertTrue(testInstance.invokeMember("test_getIssue").asBoolean());
    }

}
