package tolk.perf;

import org.graalvm.polyglot.Value;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class BasicPerformanceTest extends JolcTestBase {

    @Test
    void test() throws Exception {
        Value jolkTest = getJolkTest();
        JavaPerformanceTest javaTest = getJavaTest();
        //GraalVM usually requires 10,000+ iterations of a loop to trigger the top-tier JIT compilation. 
        long iterations = 20000;
        test(jolkTest, javaTest, "run", 7, iterations);
        test(jolkTest, javaTest, "runString", 1024, iterations);
        test(jolkTest, javaTest, "runNumerical", 1024, iterations);
        test(jolkTest, javaTest, "runFactorial", 20, iterations);
        test(jolkTest, javaTest, "runFibonacci", 7, iterations);
    }

    void test(Value jolkTest, JavaPerformanceTest javaTest, String testCase, long n, long iterations) throws Exception {
    
        // Warmup: Call the entry point multiple times to trigger Method-level JIT and stabilize Inline Caches.
        // Even though Jolk has an internal loop, calling the outer method frequently is the primary 
        // signal for GraalVM to compile the RootNode to top-tier machine code.
        for (int i = 0; i < 100; i++) {
            jolkTest.invokeMember(testCase, n, iterations / 100 > 0 ? iterations / 100 : 1);
        }
        jolkTest.invokeMember(testCase, n, iterations); // Final full-depth warmup

        Method javaMethod = javaTest.getClass().getDeclaredMethod(testCase, long.class, long.class);
        javaMethod.setAccessible(true);
        for (int i = 0; i < 100; i++) {
            javaMethod.invoke(javaTest, n, iterations / 100 > 0 ? iterations / 100 : 1);
        }
        javaMethod.invoke(javaTest, n, iterations);

        // test Jolk
        long start = System.nanoTime();
        jolkTest.invokeMember(testCase, n, iterations);
        long end = System.nanoTime();
        long jolkTime = end - start;

        // test Java 
        start = System.nanoTime();
        javaMethod.invoke(javaTest, n, iterations);
        end = System.nanoTime();   
        long javaTime = end - start;
        System.out.println("Test " + testCase + ": java: " + javaTime + " ns" + " jolk: " + jolkTime + " ns" + " jolk/java: " + ((double)jolkTime / javaTime) + "x slower");
    }

    private Value getJolkTest() {
        String source = """
            class JolkPerformanceTest {
                Long run(Long n, Long times) {
                    String s = n #toString + " - " + n #toString;
                    Long res = (n * times / 2) == 0 ? 0 : 1;
                    ^ res + self #factorial(n) + self #fibonacci(n) + s #length
                }
                Long runString(Long n, Long times) {
                    Long totalLength = 0;
                    times #times [
                        String s = n #toString + " - " + n #toString;
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
                Long fibonacci(Long n) {
                    ^ (n <= 1) ? n : self #fibonacci(n - 1) + self #fibonacci(n - 2)
                }
                Long runFibonacci(Long n, Long times) {
                    Long sum = 0;
                    times #times [ sum = sum + self #fibonacci(n) ];
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
        long run(long n, long times) {
            String s = n + " - " + n;
            long res = 0;
            if ((n * times / 2) == 0) { 
                res = 0;
            } else {
                res = 1;
            }
            return res + factorial(n) + fibonacci(n) + s.length();
        } 
        long runString(long n, long times) {
            long totalLength = 0;
            for (long i = 0; i < times; i++) {
                String s = n + " - " + n;
                totalLength += s.length();
            }
            return totalLength;
        }
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
        long runFactorial(long n, long times) {
            long sum = 0;
            for (long i = 0; i < times; i++) {
                sum += factorial(n);
            }
            return sum;
        }
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
    }
}
