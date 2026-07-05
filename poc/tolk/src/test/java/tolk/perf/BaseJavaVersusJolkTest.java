package tolk.perf;

import java.lang.reflect.Method;

import org.graalvm.polyglot.Value;

public class BaseJavaVersusJolkTest extends BasicPerformanceTest {

    public static void main(String[] args) {
        new BaseJavaVersusJolkTest().run(args);
    }
    

    protected void test() throws Exception {

        JavaPerformanceTest javaTest = getJavaTest();        
        // High iteration count ensures we spend more time in compiled code than in the JIT thread.
        long iterations = 100000;
        
        System.out.println("Benchmark               iterations   param   java          jolk          factor");
        System.out.println("----------------------|------------|-------|-------------|-------------|--------");
        
        String[] tests = {"runBase", "runString", "runNumerical", "runFactorial", "runFactorialIterative", "runFibonacci", "runFibonacciIterative"};
        long[] params = {7, 1024, 1024, 20, 20, 7, 7};

        for (int i = 0; i < tests.length; i++) {
            // RE-INITIALIZE Jolk for every test to prevent Profile Pollution
            Value freshJolk = getJolkTest();
            test(freshJolk, javaTest, tests[i], params[i], iterations);
        }
    }

    void test(Value jolkTest, JavaPerformanceTest javaTest, String testCase, long n, long iterations) throws Exception {
        Method javaMethod = javaTest.getClass().getDeclaredMethod(testCase, long.class, long.class);
        javaMethod.setAccessible(true);

        // 1. Warmup Java
        for (int i = 0; i < 10000; i++) {
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
        for (int i = 0; i < 50000; i++) {
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

        System.out.printf("%-22s %12d %7d %13d %13d %7.2fx\n", testCase, iterations, n, javaTime, jolkTime, ratio);
    }
    

    private JavaPerformanceTest getJavaTest() {
        return new JavaPerformanceTest();
    }

    private class JavaPerformanceTest {
        @SuppressWarnings("unused")
        long runBase(long n, long times) {
            String s = "Iterations: " + times;
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
