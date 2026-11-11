package tolk.perf;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.graalvm.polyglot.Value;

public class BaseJavaVersusJolkTest extends BasicPerformanceTest {

    private static final int ROUNDS = 5;
    private static volatile long SINK;

    public static void main(String[] args) {
        new BaseJavaVersusJolkTest().run(args);
    }
    

    protected void test() throws Throwable {
    
        // High iteration count ensures we spend more time in compiled code than in the JIT thread.
        long iterations = 100000;
        
        System.out.println("Benchmark               iterations   param   java          jolk          factor");
        System.out.println("----------------------|------------|-------|-------------|-------------|--------");
        
        String[] tests = {"runBase", "runString", "runNumerical", "runFactorial", "runFactorialIterative", "runFibonacci", "runFibonacciIterative"};
        long[] params = {7, 1024, 1024, 20, 20, 7, 7};

        for (int i = 0; i < tests.length; i++) {
            test(tests[i], params[i], iterations);
        }
    }

    void test(String testCase, long n, long iterations) throws Throwable {

        JavaPerformanceTest javaTest = getJavaTest();
        Method javaMethod = javaTest.getClass().getDeclaredMethod(testCase, long.class, long.class);
        javaMethod.setAccessible(true);
        // Warmup Java (same shape as measurement, a few entries)
        for (int i = 0; i < ROUNDS; i++) {
            javaMethod.invoke(javaTest, n, iterations);
        }
        System.gc();
        Thread.sleep(500);

        // Measure Java: several rounds, keep median
        long[] javaTimes = new long[ROUNDS];
        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();
            javaMethod.invoke(javaTest, n, iterations);
            javaTimes[r] = System.nanoTime() - start;
            SINK++; // opaque use: blocks DCE
        }

        Value jolkTest = getJolkTest();
        // Warmup Jolk
        for (int r = 0; r < ROUNDS; r++) jolkTest.invokeMember(testCase, n, iterations);
        System.gc();
        Thread.sleep(500);

        // Measure Jolk
        long[] jolkTimes = new long[ROUNDS];
        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();
            jolkTest.invokeMember(testCase, n, iterations);
            jolkTimes[r] = System.nanoTime() - start;
            SINK++;                      // opaque use: blocks DCE
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
