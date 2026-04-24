package tolk.perf;

import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import tolk.JolcTestBase;


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
public abstract class BasicPerformanceTest extends JolcTestBase {

    public void setUp() {
        super.setUp();
        this.checkGraalVM();
    }
    
    public void run(String[] args) {
        try {
            setUp();
            test();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tearDown();
        }
    }

    protected abstract void test() throws Exception;

    protected long measureJolk(Value jolkTest, String member, long n, long iterations) {
        long start = System.nanoTime();
        jolkTest.invokeMember(member, n, iterations);
        return System.nanoTime() - start;
    }

    protected Value getJolkTest() {
        String source = """
            class JolkPerformanceTest {
                Long runBase(Long n, Long times) {
                    String s = "Iterations: " + times;
                    Long res = (n * times / 2) == 0 ? 0 : 1;
                    ^ res + self #factorial(n) + self #fibonacci(n) + s #length
                }
                Long runString(Long n, Long times) {
                    Long totalLength = 0;
                    times #times [
                        String s = "Iterations: " + times;
                        totalLength = totalLength + s #length
                    ];
                    ^ totalLength
                }
                Long runNumerical(Long n, Long times) {
                    Long sum = 0;
                    times #times [
                        sum = sum + ((n * times / 2) == 0 ? 0 : 1)
                    ];
                    ^ sum
                }
                Long factorial(Long n) {
                    ^ (n <= 1) ? 1 : n * self #factorial(n - 1)
                }
                Long runFactorial(Long n, Long times) {
                    Long sum = 0;
                    times #times [ sum = sum + self #factorial(n) ];
                    ^ sum
                }
                Long runFactorialIterative(Long n, Long times) {
                    Long sum = 0;
                    times #times [
                        Long result = 1;
                        Long i = 2;
                        n #times [
                            result = result * i;
                            i = i + 1
                        ];
                        sum = sum + result;
                    ];
                    ^ sum
                }
                Long fibonacci(Long n) {
                    ^ (n <= 1) ? n : self #fibonacci(n - 1) + self #fibonacci(n - 2)
                }
                Long runFibonacci(Long n, Long times) {
                    Long sum = 0;
                    times #times [ sum = sum + self #fibonacci(n) ];
                    ^ sum
                }
                Long runFibonacciIterative(Long n, Long times) {
                    Long sum = 0;
                    times #times [
                        Long a = 0; Long b = 1;
                        Long i = 0;
                        n #times [
                            Long temp = a;
                            a = b;
                            b = temp + b;
                            i = i + 1
                        ];
                        sum = sum + a;
                    ];
                    ^ sum
                }
            }            
        """;
        Value meta = eval(source);
        Value jolkTest = meta.invokeMember("new");
        return jolkTest;
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
