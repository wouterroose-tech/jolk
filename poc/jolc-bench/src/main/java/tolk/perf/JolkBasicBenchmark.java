package tolk.perf;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/// # JolkBasicBenchmark
/// 
/// JMH Benchmark for Jolk vs Java performance comparisons.
/// 
/// ## Benchmark Configuration
/// - **Warmup**: 7 iterations to allow the Truffle background compiler and Graal JIT to reach peak steady-state.
/// - **Measurement**: 5 iterations to capture statistical averages once peak performance is reached.
/// - **Forks**: Increased to 3 to ensure result reproducibility and account for JVM startup variance.
/// 
/// This ensures isolated JVM forks and proper warmup for both the Host JIT and the Jolk interpreter/compiler.
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 7, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
/// @Fork(value = 1) is for development, but for final performance reporting, 
/// increase this to 3 or 5 to account for OS-level jitter and JVM start-up variance.
@Fork(value = 1, jvmArgsAppend = {"-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI"})
public class JolkBasicBenchmark {

    /// ## main
    /// 
    /// Entry point to execute the benchmark directly from an IDE or debugger.
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(JolkBasicBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    private Context context;
    private Value jolkTest;
    private final JavaBasicBenchmarkWorkload javaTest = new JavaBasicBenchmarkWorkload();

    @Param({"7"})
    public long n;

    @Param({"100000"})
    public long iterations;

    @Setup(Level.Trial)
    public void setup() {
        this.context = Context.newBuilder("jolk")
                .allowAllAccess(true)
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true)
                .build();
        this.jolkTest =  JolkBasicBenchmarkWorkload.create(context);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        this.context.close();
    }

    /// ### javaBase
    /// 
    /// Measures the baseline performance of combined algorithmic tasks implemented in Java.
    @Benchmark
    public long javaBase() {
        return javaTest.runBase(n, iterations);
    }

    /// ### jolkBase
    /// 
    /// Measures Jolk's performance on the combined algorithmic kernel, testing Truffle's ability to inline multiple calls.
    @Benchmark
    public long jolkBase() {
        return jolkTest.invokeMember("runBase", n, iterations).asLong();
    }

    /// ### javaString
    /// 
    /// Measures Java's native overhead for string concatenation and length retrieval.
    @Benchmark
    public long javaString() {
        return javaTest.runString(n, iterations);
    }

    /// ### jolkString
    /// 
    /// Measures Jolk's string operation efficiency, specifically exercising the specialized `TruffleString` nodes.
    @Benchmark
    public long jolkString() {
        return jolkTest.invokeMember("runString", n, iterations).asLong();
    }

    /// ### javaNumerical
    /// 
    /// Measures standard Java performance for pure numerical loops and primitive branching.
    @Benchmark
    public long javaNumerical() {
        return javaTest.runNumerical(n, iterations);
    }

    /// ### jolkNumerical
    /// 
    /// Measures Jolk's numerical operation efficiency, 
    @Benchmark
    public long jolkNumerical() {
        return jolkTest.invokeMember("runNumerical", n, iterations).asLong();
    }

    /// ### javaFactorialIterative
    /// 
    /// Measures the performance of iterative factorial logic on the host JVM.
    @Benchmark
    public long javaFactorialIterative() {
        return javaTest.runFactorialIterative(n, iterations);
    }

    /// ### jolkFactorialIterative
    /// 
    /// Measures iterative factorial performance in Jolk, testing the efficiency of 
    /// local variable writes and the `#times` loop optimization.
    @Benchmark
    public long jolkFactorialIterative() {
        return jolkTest.invokeMember("runFactorialIterative", n, iterations).asLong();
    }

    /// ### javaFibonacciIterative
    /// 
    /// Measures the performance of the iterative Fibonacci algorithm in Java.
    @Benchmark
    public long javaFibonacciIterative() {
        return javaTest.runFibonacciIterative(n, iterations);
    }

    /// ### jolkFibonacciIterative
    /// 
    /// Measures the performance of the iterative Fibonacci algorithm in the Jolk guest language.
    @Benchmark
    public long jolkFibonacciIterative() {
        // Replace with the actual method call on your Jolk object
        return jolkTest.invokeMember("runFibonacciIterative", n, iterations).asLong();
    }
}