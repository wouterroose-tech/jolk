package tolk.perf;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.graalvm.polyglot.Value;

public class BaseJavaVersusJolkTest extends BasicPerformanceTest {

    private static final int ROUNDS = 7;

    public static void main(String[] args) {
        new BaseJavaVersusJolkTest().run(args);
    }
    

    protected void test() throws Throwable {
    
        // High iteration count ensures we spend more time in compiled code than in the JIT thread.
        long iterations = 10000;
        
        System.out.println("Benchmark               iterations   param   java          jolk          factor");
        System.out.println("----------------------|------------|-------|-------------|-------------|--------");
        
        String[] tests = {"runBase", "runString", "runNumerical", "runFactorial", "runFactorialIterative", "runFibonacci", "runFibonacciIterative"};
        long[] params = {7, 1024, 1024, 20, 20, 7, 7};

        for (int i = 0; i < tests.length; i++) {
            test(tests[i], params[i], iterations);
        }
    }

    /**
     * Runs one benchmark case against both implementations and prints a comparison row.
     *
     * <p>Both sides must be <em>shape-identical</em>: the Java implementation and the Jolk
     * implementation must perform exactly the same work per invocation, including any nested
     * message sends. A shape mismatch silently biases the factor.</p>
     *
     * <p>Warmup and measurement use the same shape: {@link #ROUNDS} full invocations of the
     * same method with the same arguments, so Truffle compilation is triggered and completed
     * by work identical to the timed region. Warmup must never use a reduced iteration count,
     * or compilation lands inside the timed region and inflates the measured time.</p>
     *
     * <p>Rounds are interleaved (one Java round, then one Jolk round) so both engines are
     * measured under equal CPU, thermal and background-compilation conditions. Each round is
     * preceded by {@link System#gc()} and a settle pause, because first-time compilation or a
     * collection inside a timed round otherwise poisons it. The reported time per side is the
     * median of the {@link #ROUNDS} rounds; medians of fewer than five rounds are dominated by
     * single-round outliers.</p>
     *
     * <p>Times are nanoseconds as returned by {@link System#nanoTime()} around a single
     * reflective/polyglot invocation. Factors below roughly 10x carry run-to-run noise on this
     * hardware; use JMH (forking plus blackholed results) before quoting Java-baseline factors
     * externally.</p>
     *
     * @param testCase   name of the declared method to benchmark; must exist on both the Java
     *                   test class and the Jolk test object with signature
     *                   {@code (long, long)}
     * @param n          the workload parameter passed through to both implementations
     *                   (e.g. recursion depth or string length)
     * @param iterations the in-guest loop count passed through to both implementations;
     *                   both implementations must loop in-guest over this count
     */
    void test(String testCase, long n, long iterations) throws Throwable {

        JavaPerformanceTest javaTest = getJavaTest();
        Method javaMethod = javaTest.getClass().getDeclaredMethod(testCase, long.class, long.class);
        javaMethod.setAccessible(true);
        Value jolkTest = getJolkTest();

        // Warmup (same shape as measurement, a few entries)
        for (int i = 0; i < 3; i++) {
            javaMethod.invoke(javaTest, n, iterations);
            jolkTest.invokeMember(testCase, n, iterations);
        }
        
        long[] javaTimes = new long[ROUNDS];
        long[] jolkTimes = new long[ROUNDS];
        // Measure several rounds, keep median
        for (int r = 0; r < ROUNDS; r++) {
            System.gc();
            Thread.sleep(500);
            // Measure Java
            long start = System.nanoTime();
            javaMethod.invoke(javaTest, n, iterations);
            javaTimes[r] = System.nanoTime() - start;

            // Measure Jolk
            System.gc();
            Thread.sleep(500);
            start = System.nanoTime();
            jolkTest.invokeMember(testCase, n, iterations);
            jolkTimes[r] = System.nanoTime() - start;
        }

        long javaTime = median(javaTimes);
        long jolkTime = median(jolkTimes);
        System.out.printf("%-22s %12d %7d %13d %13d %7.2fx%n",
                testCase, iterations, n, javaTime, jolkTime, (double) jolkTime / javaTime);
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        int mid = sorted.length / 2;
        return (sorted.length % 2 == 0)
                ? sorted[mid - 1] / 2 + sorted[mid] / 2   // avoid overflow on large sums
                : sorted[mid];
    }
    

    private JavaPerformanceTest getJavaTest() {
        return new JavaPerformanceTest();
    }

    private class JavaPerformanceTest {
        @SuppressWarnings("unused")
        long runBase(long n, long times) {
            String s = "Iterations: " + times;
            long res =  ((n * times / 2) == 0) ? 0 : 1;
            return res + factorial(n) + fibonacci(n) + s.length();
        } 
        @SuppressWarnings("unused")
        long runString(long n, long times) {
            long totalLength = 0;
            for (long i = 0; i < times; i++) {
                String s = "Iterations: " + times;
                totalLength += s.length();
            }
            return totalLength;
        }
        @SuppressWarnings("unused")
        long runNumerical(long n, long times) {
            long sum = 0;
            for (long i = 0; i < times; i++) {
                sum += ((i * times % 2) == 0) ? 0 : 1;
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
