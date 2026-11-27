package runner.basic;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.graalvm.polyglot.Value;

import basic.BasicBenchmark;
import runner.JolkRuntime;

public class RunBasicBenchmark extends BaseRunner {

    private static final int ROUNDS = 7;

    public static void main(String[] args) {
        new RunBasicBenchmark().run(args);
    }

    @Override 
    protected void run(JolkRuntime jolk) throws Throwable {
    
        // High iteration count ensures we spend more time in compiled code than in the JIT thread.
        long iterations = 7000;
        
        System.out.println("Benchmark               iterations   param   java          jolk          factor");
        System.out.println("----------------------|------------|-------|-------------|-------------|--------");
        
        String[] tests = {"runBase", "runString", "runNumerical", "runFactorial", "runFactorialIterative", "runFibonacci", "runFibonacciIterative"};
        long[] params = {7, 1024, 1024, 20, 20, 7, 7};

        BasicBenchmark javaTest = new BasicBenchmark();
        Value jolkTest = jolk.createInstance("/basic/BasicBenchmark.jolk");

        for (int i = 0; i < tests.length; i++) {
            run(tests[i], params[i], iterations, javaTest, jolkTest);
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
     * @param javaTest 
     * @param jolkTest 
     */
    void run(String testCase, long n, long iterations, Object javaTest, Value jolkTest) throws Throwable {

        Method javaMethod = javaTest.getClass().getDeclaredMethod(testCase, long.class, long.class);
        javaMethod.setAccessible(true);
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
            //System.out.println("round: " + r);
            jolkTimes[r] = System.nanoTime() - start;
        }

        long javaTime = trimmedMedian(javaTimes);
        long jolkTime = trimmedMedian(jolkTimes);
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

    // remove min() & max() and return median
    public static long trimmedMedian(long[] times) {
        if (times.length < 3) {
            throw new IllegalArgumentException("Array length must be at least 3 to trim extremes.");
        }
        long[] sorted = times.clone();
        java.util.Arrays.sort(sorted);
        
        int start = 1;
        int end = sorted.length - 1;
        int length = end - start;
        
        if (length % 2 == 1) {
            return sorted[start + length / 2];
        } else {
            return (sorted[start + length / 2 - 1] + sorted[start + length / 2]) / 2;
        }
    }

}
