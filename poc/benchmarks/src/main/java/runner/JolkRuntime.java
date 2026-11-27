package runner;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import tolk.language.JolkLanguage;

/// # JolkBasicBenchmarkWorkload
/// 
/// Provides the Jolk guest-language implementation for fundamental performance benchmarks.
///
///
/// set these argumens to activate GraalJIT
///    vmArgs:
///     -XX:+UnlockExperimentalVMOptions,
///     -XX:+EnableJVMCI,
///     -XX:+UseJVMCICompiler 
///     -Xms4G -Xmx4G
///     -XX:MetaspaceSize=512m
///     -XX:ReservedCodeCacheSize=512m
///     -XX:+HeapDumpOnOutOfMemoryError
/// 
public class JolkRuntime implements AutoCloseable {

    private Engine engine;
    private Context context;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    // ? map Name -> JolkC-Class

    public JolkRuntime() {
        this.out = new ByteArrayOutputStream();
        this.err = new ByteArrayOutputStream();
        this.engine = getEngine();
        this.context = createContext();
        this.checkGraalVM();
    }

    protected Engine getEngine() {
        return Engine.newBuilder()
                .allowExperimentalOptions(true)
                // allow System #out #println
                //.out(out)
                .err(err)
                .build();
    }

    private Context createContext() {
        return Context.newBuilder(JolkLanguage.ID)
                .engine(engine)
                .allowAllAccess(true)
                // Allow Jolk to access all public host (Java) members
                .allowHostAccess(HostAccess.ALL)
                 // Allow Jolk to look up any Java class;
                .allowHostClassLookup(className -> true)
                .build();
    }

    public void close() {
        if (context != null) {
            context.close();
        }
        if (engine != null) { // engine can be null if setUp fails
            engine.close();
        }
    }

    public Value loadClass(String className, String path) {
        String source = readResource(path);
        return context.eval(JolkLanguage.ID, source);
    }

    public Value createInstance(String className, String path) {
        return loadClass(className, path).invokeMember("new");
    }

    /// ### readResource
    /// 
    /// Utility to read a Jolk source file from the classpath resources.
    protected static String readResource(String path) {
        // Try to load via the thread context class loader (robust for polyglot scenarios)
        String cpPath = path.startsWith("/") ? path.substring(1) : path;
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(cpPath);
        
        if (is == null) {
            // Fallback to instance class-relative lookup
            is = JolkRuntime.class.getResourceAsStream(path);
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
    

    /**
     * ### checkGraalVM
     *
     * Verifies if the current JVM is a GraalVM instance and prints environment details.
     * High-performance Jolk execution requires the Graal JIT compiler to perform
     * **Partial Evaluation** and **Instructional Projection**.
     */
    private void checkGraalVM() {
        String vmName = System.getProperty("java.vm.name");
        String vmVersion = System.getProperty("java.vm.version");
        String javaHome = System.getProperty("java.home");
        String vendor = System.getProperty("java.vendor");
        
        System.out.println("Runtime: " + vmName + " | Version: " + vmVersion);
        System.out.println("Vendor: " + vendor);
        System.out.println("Java Home: " + javaHome);
        
        boolean isJVMCI = vmVersion.contains("jvmci") || vmName.contains("GraalVM");

        if (!isJVMCI) {
            System.err.println("----------------------------------------------------------------------");
            System.err.println("WARNING: NOT RUNNING ON GRAALVM.");
            System.err.println("Jolk code will be interpreted. Expect 10x-500x slower execution.");
            System.err.println("To fix: Ensure %JAVA_HOME% is at the top of your system PATH.");
            System.err.println("----------------------------------------------------------------------");
            return;
        }

        try (Engine engine = Engine.create()) {
            String impl = engine.getImplementationName();
            String version = engine.getVersion();
            System.out.println("Polyglot Engine: " + impl + " | Version: " + version);
            
            if (impl.contains("Interpreted") || impl.contains("fallback")) {
                System.err.println("----------------------------------------------------------------------");
                System.err.println("CRITICAL: FALLBACK RUNTIME DETECTED (No JIT).");
                System.err.println("The JVMCI flags are present, but the Truffle compiler is missing.");
                System.err.println("Action: Ensure 'org.graalvm.truffle:truffle-runtime' is on the classpath.");
                System.err.println("----------------------------------------------------------------------");
            } else {
                System.out.println("SUCCESS: Graal JIT is active for Jolk execution.");
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize Polyglot Engine: " + e.getMessage());
        }
    }
}
