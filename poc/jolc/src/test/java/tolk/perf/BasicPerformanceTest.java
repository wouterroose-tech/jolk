package tolk.perf;

import org.graalvm.polyglot.Value;

import java.lang.reflect.Method;

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
public class BasicPerformanceTest extends JolcTestBase {

    public static void main(String[] args) {
        BasicPerformanceTest test = new BasicPerformanceTest();
        try {
            test.setUp();
            test.test();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            test.tearDown();
        }
    }

    //@Test
    void test() throws Exception {
        checkGraalVM();

        Value jolkTest = getJolkTest();
        JavaPerformanceTest javaTest = getJavaTest();

        // Warmup Phase: Gradually increase iterations to trigger JIT compilation and observe performance trends.
        this.warmup(jolkTest);
        
        // High iteration count ensures we spend more time in compiled code than in the JIT thread.
        long iterations = 50000;
        
        System.out.println("Benchmarking               iterations   param   java          jolk          factor");
        System.out.println("-------------------------|------------|-------|-------------|-------------|--------");
        test(jolkTest, javaTest, "run", 7, iterations);
        test(jolkTest, javaTest, "runString", 1024, iterations);
        test(jolkTest, javaTest, "runNumerical", 1024, iterations);
        test(jolkTest, javaTest, "runFactorial", 20, iterations);
        test(jolkTest, javaTest, "runFactorialIterative", 20, iterations);
        test(jolkTest, javaTest, "runFibonacci", 7, iterations);
        test(jolkTest, javaTest, "runFibonacciIterative", 7, iterations);
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
        
        boolean isGraal = vmName.contains("GraalVM") || vmVersion.contains("GraalVM") || 
                          vmVersion.contains("jvmci") || javaHome.toLowerCase().contains("graalvm");

        if (!isGraal) {
            System.err.println("----------------------------------------------------------------------");
            System.err.println("WARNING: NOT RUNNING ON GRAALVM.");
            System.err.println("Jolk code will be interpreted. Expect 10x-500x slower execution.");
            System.err.println("To fix: Ensure %JAVA_HOME% is at the top of your system PATH.");
            System.err.println("----------------------------------------------------------------------");
        }
    }

    void test(Value jolkTest, JavaPerformanceTest javaTest, String testCase, long n, long iterations) throws Exception {
        Method javaMethod = javaTest.getClass().getDeclaredMethod(testCase, long.class, long.class);
        javaMethod.setAccessible(true);

        // 1. Warmup Java
        for (int i = 0; i < 5000; i++) {
            javaMethod.invoke(javaTest, n, 1);
        }

        // Engineered Integrity: Ensure the JIT compilation threads have finished
        // and the GC has reclaimed any temporary warmup objects before measuring.
        System.gc();
        Thread.sleep(500); 
    
        // 2. Measure Java
        long start = System.nanoTime();
        javaMethod.invoke(javaTest, n, iterations);
        long javaTime = System.nanoTime() - start;

        // 3. Warmup Jolk (Trigger Truffle Compilation)
        // We need many entries to the RootNode to trigger Graal JIT.
        // Compilation Thresholds: Truffle typically waits for a method to be called 1,000 times (default) before compiling it.
        for (int i = 0; i < 30000; i++) {
            jolkTest.invokeMember(testCase, n, 1);
        }

        // reset before measuring Jolk
        System.gc();
        Thread.sleep(500); 

        // 4. Measure Jolk
        start = System.nanoTime();
        jolkTest.invokeMember(testCase, n, iterations);
        long jolkTime = System.nanoTime() - start;
        double ratio = (double) jolkTime / javaTime;

        System.out.printf("%-25s %12d %7d %13d %13d %7.2fx\n", testCase, iterations, n, javaTime, jolkTime, ratio);
    }

    private void warmup(Value jolkTest) throws InterruptedException {

        System.out.println("iterations   run          runString    runNumerical runFibonacci");
        System.out.println("------------|------------|------------|------------|------------");


        int it = 1;
        for (int i = 1; i <= 16; i++) {
            it = it * 2;
            long tRun = measureJolk(jolkTest, "run", 7, it);
            long tString = measureJolk(jolkTest, "runString", 1024, it);
            long tNumerical = measureJolk(jolkTest, "runNumerical", 1024, it);
            long tFib = measureJolk(jolkTest, "runFibonacci", 7, it);

            System.out.printf("%12d|%12d|%12d|%12d|%12d\n", it, tRun / it, tString / it, tNumerical / it, tFib / it);
        System.gc();
        Thread.sleep(100); 
        }
        System.out.println();
    }

    private long measureJolk(Value jolkTest, String member, long n, long iterations) {
        long start = System.nanoTime();
        jolkTest.invokeMember(member, n, iterations);
        return System.nanoTime() - start;
    }

    private Value getJolkTest() {
        String source = """
            class JolkPerformanceTest {
                Long run(Long n, Long times) {
                    String s = "" + n + " - " + n;
                    Long res = (n * times / 2) == 0 ? 0 : 1;
                    ^ res + self #factorial(n) + self #fibonacci(n) + s #length
                }
                Long runString(Long n, Long times) {
                    Long totalLength = 0;
                    times #times [
                        String s = "" + n + " - " + n;
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

    private JavaPerformanceTest getJavaTest() {
        return new JavaPerformanceTest();
    }

    private class JavaPerformanceTest {
        @SuppressWarnings("unused")
        long run(long n, long times) {
            String s = "" + n + " - " + n;
            long res = 0;
            if ((n * times / 2) == 0) { 
                res = 0;
            } else {
                res = 1;
            }
            return res + factorial(n) + fibonacci(n) + s.length();
        } 
        @SuppressWarnings("unused")
        long runString(long n, long times) {
            long totalLength = 0;
            for (long i = 0; i < times; i++) {
                String s = "" + n + " - " + n;
                totalLength += s.length();
            }
            return totalLength;
        }
        @SuppressWarnings("unused")
        long runNumerical(long n, long times) {
            long sum = 0;
            for (long i = 0; i < times; i++) {
                sum += ((n * times / 2) == 0) ? 0 : 1;
            }
            return sum;
        }
        long factorial(long n) {
            return (n <= 1) ? 1 : n * factorial(n - 1);
        }
        @SuppressWarnings("unused")
        long runFactorial(long n, long times) {
            long sum = 0;
            for (long i = 0; i < times; i++) {
                sum += factorial(n);
            }
            return sum;
        }
        @SuppressWarnings("unused")
        long runFactorialIterative(long n, long times) {
            long sum = 0;
            for (long i = 0; i < times; i++) {
                long result = 1;
                for (long j = 2; j <= n; j++) {
                    result *= j;
                }
                sum += result;
            }
            return sum;
        }
        @SuppressWarnings("unused")
        long runFibonacci(long n, long times) {
            long sum = 0;
            for (long i = 0; i < times; i++) {
                sum += fibonacci(n);
            }
            return sum;
        }
        long fibonacci(long n) {
            return (n <= 1) ? n : fibonacci(n - 1) + fibonacci(n - 2);
        }

        @SuppressWarnings("unused")
        long runFibonacciIterative(long n, long times) {
            long sum = 0;
            for (long i = 0; i < times; i++) {
                long a = 0; long b = 1;
                for (long j = 0; j < n; j++) {
                    long temp = a;
                    a = b;
                    b = temp + b;
                }
                sum += a;
            }
            return sum;
        }
    }
}
