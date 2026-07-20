package test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tolk.language.JolkLanguage;

/// # TestRunner
///
/// run the jolk TestRunner, will be replaced by the JolkTestEngine for the JUnit test framework
///
/// @author Wouter Roose
///
public class TestRunner  {
    
    protected Value test;
    protected Engine engine;
    protected Context context;
    protected ByteArrayOutputStream out;
    protected ByteArrayOutputStream err;
    protected Value testInstance;

    public void setUp(String path) {
        testInstance = load(path).invokeMember("new");
    }

    protected Engine.Builder getEngine() {
        return Engine.newBuilder()
                .allowExperimentalOptions(true)
                // allow System #out #println
                //.out(out)
                .err(err);
    }

    protected Context.Builder getContext() {
        return Context.newBuilder(JolkLanguage.ID)
                .engine(engine)                
                .allowAllAccess(true)
                // Allow Jolk to access all public host (Java) members
                .allowHostAccess(HostAccess.ALL) 
                 // Allow Jolk to look up any Java class;
                .allowHostClassLookup(className -> true);
    }


    @AfterEach
    public void tearDown() {
        if (context != null) { // context can be null if setUp fails
            context.close();
        }
        if (engine != null) { // engine can be null if setUp fails
            engine.close();
        }
    }

    protected Value eval(String source) {
        return context.eval(JolkLanguage.ID, source);
    }

    /// ### readResource
    /// 
    /// Utility to read a Jolk source file from the classpath resources.
    protected String readResource(String path) {
        // Try to load via the thread context class loader (robust for polyglot scenarios)
        String cpPath = path.startsWith("/") ? path.substring(1) : path;
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(cpPath);
        
        if (is == null) {
            // Fallback to instance class-relative lookup
            is = getClass().getResourceAsStream(path);
        }

        if (is == null) {
            throw new RuntimeException("Resource not found on classpath: " + path );
        }

        try (InputStream stream = is) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }

    /// ### load
    ///
    /// Helper method to load the class from the Jolk source file for testing purposes.
    protected Value load(String path) {
        String source = readResource(path);
        return eval(source);
    }

    @BeforeEach
    public void setUp() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        engine = getEngine().build();
        context = getContext().build();

        // test framework
        load("/jolk/test/api/Test.jolk");
        load("/jolk/test/api/TestCase.jolk");
        load("/jolk/test/api/TestSuite.jolk");
        load("/jolk/test/api/TestResult.jolk");
        load("/jolk/test/api/TestStatus.jolk");
        load("/jolk/test/api/AssertionSignal.jolk");
        load("/jolk/test/api/DisabledSignal.jolk");
        load("/jolk/test/api/TimeoutSignal.jolk");
        load("/jolk/test/api/TestCase_Test.jolk");
        load("/jolk/test/api/TestSuite_Test.jolk");
        load("/jolk/test/api/TestResult_Test.jolk");
        load("/jolk/test/engine/TestRunner_Test.jolk");
    }
    
    @Test
    public void testRun() {
        load("/jolk/test/engine/TestRunner.jolk")
            .invokeMember("new")
            .invokeMember("run");
    }

}
