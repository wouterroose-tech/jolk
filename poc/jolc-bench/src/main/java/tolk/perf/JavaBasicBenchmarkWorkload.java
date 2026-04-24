package tolk.perf;

/**
 * # JavaBasicBenchmarkWorkload
 * 
 * Provides the baseline Java implementation for fundamental performance benchmarks.
 */
public class JavaBasicBenchmarkWorkload {

    /**
     * ### runBase
     * 
     * Executes a composite workload involving strings, basic arithmetic, and recursion.
     */
    long runBase(long n, long times) {
        String s = "Iterations: " + times;
        long res = ((n * times / 2) == 0) ? 0 : 1;
        return res + factorial(n) + fibonacci(n) + s.length();
    }

    /**
     * ### runString
     * 
     * Focuses on string allocation and iteration overhead.
     */
    long runString(long n, long times) {
        long totalLength = 0;
        for (long i = 0; i < times; i++) {
            String s = "Iterations: " + times;
            totalLength += s.length();
        }
        return totalLength;
    }

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

    long fibonacci(long n) {
        return (n <= 1) ? n : fibonacci(n - 1) + fibonacci(n - 2);
    }

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
