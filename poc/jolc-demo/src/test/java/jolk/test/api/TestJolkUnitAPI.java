package jolk.test.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.graalvm.polyglot.Value;

import util.JolkTestBase;

public class TestJolkUnitAPI extends JolkTestBase {
    
    protected Value test;

    @BeforeEach
    public void setUp() {
        super.setUp();
        super.setUp("/test/api/JolkUnitTest.jolk");
    }
    
    @Test
    public void testSuccess() {
        test("testSuccess");
    }
    
    @Test
    public void testFail() {
        assertThrows(RuntimeException.class, () -> test.invokeMember("testFail"));
    }
    
    @Test
    public void testAssertThat_success() {
        test("testAssertThat_success");
    }
    
    @Test
    public void testAssertThat_failure() {
        assertThrows(RuntimeException.class, () -> test.invokeMember("testAssertThat_failure"));
    }
    
    @Test
    public void testAssertEquals_success() {
        test("testAssertEquals_success");
    }
    
    @Test
    public void testAssertEquals_failure() {
        assertThrows(RuntimeException.class, () -> test.invokeMember("testAssertEquals_failure"));
    }
    
    @Test
    public void testAssertIsPresent_success() {
        test("testAssertIsPresent_success");
    }
    
    @Test
    public void testAssertIsPresent_failure() {
        assertThrows(RuntimeException.class, () -> test.invokeMember("testAssertIsPresent_failure"));
    }

}
