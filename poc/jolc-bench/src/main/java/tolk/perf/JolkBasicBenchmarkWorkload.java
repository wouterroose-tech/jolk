package tolk.perf;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * # JolkBasicBenchmarkWorkload
 * 
 * Provides the Jolk guest-language implementation for fundamental performance benchmarks.
 */
public class JolkBasicBenchmarkWorkload {


    static Value create(Context context) {
        Value meta = context.eval("jolk", source);
        return meta.invokeMember("new");
    }

    static final String source = """
        class JolkBasicBenchmarkWorkload {
            /**
             * ### runBase
             * 
             * Combined kernel using recursion and string messages.
             */
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
                times #times [ i ->  sum = sum + ((i * times % 2) == 0 ? 0 : 1) ];
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
                    n #times [ i -> result = result * (i + 2) ];
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
                    n #times [ i ->
                        Long temp = a;
                        a = b;
                        b = temp + b
                    ];
                    sum = sum + a;
                ];
                ^ sum
            }
        }            
        """;
}
