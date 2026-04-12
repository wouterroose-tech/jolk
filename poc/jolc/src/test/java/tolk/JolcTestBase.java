package tolk;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import tolk.language.JolkLanguage;

import java.io.ByteArrayOutputStream;

///
/// Base class for Jolk tests that sets up a Truffle context and engine for evaluating Jolk code.
/// This class provides common setup and teardown logic, as well as a helper method for evaluating 
/// source code within the context of the tests.
///
public abstract class JolcTestBase {

    protected Engine engine;
    protected Context context;
    protected ByteArrayOutputStream out;
    protected ByteArrayOutputStream err;

    @BeforeEach
    public void setUp() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        engine = Engine.newBuilder()
                .out(out)
                .err(err)
                .build();
        context = Context.newBuilder(JolkLanguage.ID)
                .engine(engine)
                .allowAllAccess(true)
                .allowHostAccess(HostAccess.ALL) // Allow Jolk to access all public host (Java) members
                .allowHostClassLookup(className -> true) // Allow Jolk to look up any Java class
                .build();
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
}
