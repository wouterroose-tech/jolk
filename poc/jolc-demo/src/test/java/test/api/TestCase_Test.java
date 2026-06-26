package test.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.graalvm.polyglot.Value;

import util.JolkTestBase;

public class TestCase_Test extends JolkTestBase {
    
    protected Value test;

    @BeforeEach
    public void setUp() {
        super.setUp();
        super.setUp("/test/api/TestCase_Test.jolk");
    }
    
    @Test
    public void testTestProtocol() {
        test("testTestProtocol");
    }
    
    @Test
    public void testSuccess() {
        test("testSuccess");
    }
    
    @Test
    public void testFail() {
        test("testFail");
    }
    
    @Test
    public void testAssertThat_success() {
        test("testAssertThat_success");
    }
    
    @Test
    public void testAssertThat_failure() {
        test("testAssertThat_failure");
    }
    
    @Test
    public void testAssertSame_success() {
        test("testAssertSame_success");
    }
    
    @Test
    public void testAssertSame_failure() {
        test("testAssertSame_failure");
    }
    
    @Test
    public void testAssertEquals_success() {
        test("testAssertEquals_success");
    }
    
    @Test
    public void testAssertEquals_failure() {
        test("testAssertEquals_failure");
    }
    
    @Test
    public void testAssertIsPresent_success() {
        test("testAssertIsPresent_success");
    }
    
    @Test
    public void testAssertIsPresent_failure() {
        test("testAssertIsPresent_failure");
    }
    
    @Test
    public void testParameterized() {
        test("testParameterized");
    }

}
