package jolk.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tolk.language.JolkLanguage;

/// # TestRunner
///
/// run the jolk TestRunner
///
/// @author Wouter Roose
///
// Disabled, test now run via the JolkTestEngine for the JUnit test framework
@Disabled
public class TestJolkTestFramework extends TestRunner  {
    
    @Test
    public void testTestCase_Test() {
        Value testClass = load("/jolk/test/api/TestCase_Test.jolk");
        runTestClass(testClass);
    }
    
    @Test
    public void testTestResult_Test() {
        Value testClass = load("/jolk/test/api/TestResult_Test.jolk");
        runTestClass(testClass);
    }
    
    @Test
    public void testTestRunner_Test() {
        Value testClass = load("/jolk/test/engine/TestRunner_Test.jolk");
        runTestClass(testClass);
    }

}
