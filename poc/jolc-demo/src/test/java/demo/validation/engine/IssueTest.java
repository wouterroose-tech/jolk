package demo.validation.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import demo.validation.rules.ValidationTestBase;


public class IssueTest extends ValidationTestBase {


    Value testInstance;

    @BeforeEach
    public void setUp() {
        super.setUp();
        // Create the Jolk test instance
        Value testclass = getJolkClass("/test/validation/engine/IssueTest.jolk");
        testInstance = testclass.invokeMember("new");
    }

    @Test
    public void test_getIssue() {
        assertTrue(testInstance.invokeMember("test_getIssue").asBoolean());
    }

}
