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

    @BeforeEach
    public void setUp() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        engine = getEngine().build();
        context = getContext().build();
    }

    protected Engine.Builder getEngine() {
        return Engine.newBuilder()
                .allowExperimentalOptions(true)
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
            throw new RuntimeException("Resource not found on classpath: " + path + 
                ". Verify that the file is located at 'src/test/resources" + (path.startsWith("/") ? "" : "/") + path + "'");
        }

        try (InputStream stream = is) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }

    /// ### getJolkClass
    ///
    /// Helper method to load the class from the Jolk source file for testing purposes.
    protected Value getJolkClass(String path) {
        String source = readResource(path);
        return eval(source);
    }
}
