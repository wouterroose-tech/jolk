package util;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import tolk.language.JolkLanguage;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

///
/// Base class for Jolk tests that sets up a Truffle context and engine for evaluating Jolk code.
/// This class provides common setup and teardown logic, as well as a helper method for evaluating 
/// source code within the context of the tests.
///
public abstract class JolkTestBase {

    protected Engine engine;
    protected Context context;
    protected ByteArrayOutputStream out;
    protected ByteArrayOutputStream err;
    protected Value testInstance;

    @BeforeEach
    public void setUp() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        engine = getEngine().build();
        context = getContext().build();
        load("/test/api/TestCase.jolk");
        load("/test/api/TestSuite.jolk");
        load("/test/api/TestResult.jolk");
        load("/test/api/AssertionSignal.jolk");
        load("/test/api/DisabledSignal.jolk");
        load("/test/api/TimeoutSignal.jolk");
    }

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

    protected Value test(String testCase) {
        testInstance.invokeMember("before");
        try {
            return testInstance.invokeMember(testCase);
        } finally {
            testInstance.invokeMember("after");
        }
    }
}
