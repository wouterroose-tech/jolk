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
        load("/test/api/Test.jolk");
        load("/test/api/TestCase.jolk");
        load("/test/api/TestSuite.jolk");
        load("/test/api/TestResult.jolk");
        load("/test/api/TestStatus.jolk");
        load("/test/api/AssertionSignal.jolk");
        load("/test/api/DisabledSignal.jolk");
        load("/test/api/TimeoutSignal.jolk");
        load("/test/api/TestCase_Test.jolk");
        load("/test/api/TestSuite_Test.jolk");
        load("/test/api/TestResult_Test.jolk");
        // examples
        load("/examples/Circle.jolk");
        load("/examples/CircleTest.jolk");
        // demonstrators
        load("/demonstrators/ArchetypeClassDemonstrator.jolk");
        load("/demonstrators/ArchetypeClassDemonstratorTest.jolk");
        // domain
        load("/demo/validation/domain/Person.jolk");
        load("/demo/validation/domain/PersonTest.jolk");
        load("/demo/validation/domain/ContactForm.jolk");
        load("/demo/validation/domain/ContactFormTest.jolk");
        // validation engine
        load("/demo/validation/engine/Level.jolk");
        load("/demo/validation/engine/LevelTest.jolk");
        load("/demo/validation/engine/Issue.jolk");
        load("/demo/validation/engine/IssueTest.jolk");
        load("/demo/validation/engine/Interrupt.jolk");
        load("/demo/validation/engine/ExecutionContext.jolk");
        load("/demo/validation/engine/Node.jolk");
        load("/demo/validation/engine/ChildValidation.jolk");
        load("/demo/validation/engine/ChildrenValidation.jolk");
        load("/demo/validation/engine/Validation.jolk");
        load("/demo/validation/engine/Constraint.jolk");
        load("/demo/validation/engine/ValidationSuite.jolk");
        // business services
        load("/demo/validation/services/City.jolk");
        load("/demo/validation/services/GeoGraphicalService.jolk");
        // business validation rules
        load("/demo/validation/rules/SsnConstraint.jolk");
        load("/demo/validation/rules/SsnConstraintTest.jolk");
        load("/demo/validation/rules/ZipConstraint.jolk");
        load("/demo/validation/rules/ZipConstraintTest.jolk");
        load("/demo/validation/rules/ContactFormValidation.jolk");
        load("/demo/validation/rules/ContactFormValidationTest.jolk");
    }
    
    @Test
    public void testRun() {
        Value runner = load("/test/engine/TestRunner.jolk").invokeMember("new");
        runner.invokeMember("run");
    }

}
