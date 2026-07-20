package jolk.test.engine;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import tolk.language.JolkLanguage;
import tolk.runtime.JolkMetaClass;

/// Manages the lifecycle of the Tolk execution environment during the test discovery and 
/// execution phases.
///
/// <p>This class acts as the central gatekeeper for the GraalVM polyglot infrastructure, 
/// encapsulating the {@link org.graalvm.polyglot.Engine} and {@link org.graalvm.polyglot.Context} 
/// instances specifically configured for language testing. It ensures the isolation of the 
/// guest-language state and provides the mechanisms to parse, evaluate, and query Jolk source 
/// files via the Jolk meta-level protocol.</p>
///
/// <h3>Lifecycle & Phased Architecture</h3>
/// <p>The context spans across both primary phases of the JUnit Platform lifecycle:</p>
/// <ol>
/// <li><b>Discovery Phase:</b> Instantiated at the beginning of test discovery. It immediately 
/// bootstraps the core test framework ({@code TestCase.jolk}) to establish the base protocols 
/// before evaluating user-space test suites. It is then used to query JoMoo meta-objects 
/// to dynamically build the JUnit {@code TestDescriptor} tree.</li>
/// <li><b>Execution Phase:</b> Transferred via the root descriptor into the 
/// {@code JolkTestEngineExecutionContext}, providing the execution nodes with the 
/// identical, pre-warmed polyglot context containing all loaded domain and test classes.</li>
/// </ol>
///
/// <h3>Thread Safety & Isolation</h3>
/// <p>Instances of this class are bound to the executing test thread pool. No thread-safe 
/// guarantees are provided for concurrent execution within a single context instance, matching 
/// the underlying constraints of the GraalVM polyglot context.</p>
///
public class JolkTestRuntimeContext {

    protected Engine engine;
    protected Context context;
    protected ByteArrayOutputStream out;
    protected ByteArrayOutputStream err;

    public JolkTestRuntimeContext() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        engine = getEngine().build();
        context = getContext().build();
    }

    /// ### load
    ///
    /// Load the class from the Jolk source file.
    protected Value load(String path) {
        return eval(readResource(path));
    }

    public JolkMetaClass getDefinedClass(String className) {
        // Enter the Polyglot context before interacting with the guest language's internal context
        this.context.enter();
        try {
            return (JolkMetaClass) JolkLanguage.getContext().getDefinedClass(className);
        } finally {
            // Always ensure the context is left, even if an error occurs
            this.context.leave();
        }
    }

    private Engine.Builder getEngine() {
        return Engine.newBuilder()
                .allowExperimentalOptions(true)
                .out(out)
                .err(err);
    }

    private Context.Builder getContext() {
        return Context.newBuilder(JolkLanguage.ID)
                .engine(engine)                
                .allowAllAccess(true)
                // Allow Jolk to access all public host (Java) members
                .allowHostAccess(HostAccess.ALL) 
                 // Allow Jolk to look up any Java class;
                .allowHostClassLookup(className -> true);
    }

    /// ### readResource
    /// 
    /// Read a Jolk source file from the classpath resources.
    private String readResource(String path) {
        // 1. Try to read from classpath resources
        String cpPath = path.startsWith("/") ? path.substring(1) : path;
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(cpPath)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            // Log this but continue to try file system
            System.err.println("Warning: Failed to read classpath resource '" + path + "': " + e.getMessage());
        }

        // Fallback: try to read directly from the file system
        try {
            Path filePath = Paths.get(path); // Use Paths.get()
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read source from classpath or file system: " + path, e);
        }
    }

    /// Evaluate a Jolk source string and return the resulting Truffle Polyglot Value.
    private Value eval(String source) {
        return context.eval(JolkLanguage.ID, source);
    }

    public List<Path> scanDirectoryForJolkSources(Path dirPath) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'scanDirectoryForJolkSources'");
    }

    public boolean conformsToTestProtocol(JolkMetaClass metaClass) {
        // alternative: extends TestCase
        return getTestSelectors(metaClass).findAny() != null;
    }

    public Stream<String> getTestSelectors(JolkMetaClass metaClass) {
        // In future releases memebers will be filtered based on the @Test annotation and other test protocol rules, 
        // but for now we just return all members that start with "test"
        return metaClass
            .getInstanceMemberKeys()
            .stream()
            .filter(s -> s.startsWith("test"));
    }
    
    /// Evaluate the source file into Truffle to register the live MetaClass object
    public JolkMetaClass evaluateJolkSource(Path filePath) {
        Value metaClass = load(filePath.toString()); 
        return getDefinedClass(metaClass.invokeMember("qualifiedName").asString());
    }

    public Object invokeJolkTestRunner(JolkMetaClass guestTestClass, String selector) {
        // TestResult result = testClass #new #selector(s) #run;
        Object testInstance;
        try {
            testInstance = guestTestClass.callMetaMember("new");
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate test class: " + guestTestClass.name, e);
        }
        return context.asValue(testInstance).invokeMember(selector);
    }

    public void loadDirectory(String directory) {
        // Ensure path starts without a leading slash for ClassLoader resolution
        String cleanDir = directory.startsWith("/") ? directory.substring(1) : directory;

        try {
            Path path = Paths.get("target/classes", cleanDir);
            loadDirectory(path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load from: " + directory, e);
        }
    }

    private void loadDirectory(Path rootPath) throws URISyntaxException, IOException {
        try (Stream<Path> stream = Files.walk(rootPath)) {
            stream
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".jolk"))
                .sorted(Comparator.comparing(Path::toString))
                .map(path -> path.toString())
                .forEach(path -> load(path));
        }
    }

}