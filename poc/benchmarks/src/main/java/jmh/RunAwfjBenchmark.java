package jmh;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import awfj.List;
import runner.JolkRuntime;

import java.util.concurrent.TimeUnit;

/// # JolkBasicBenchmark
/// 
/// JMH Benchmark for Jolk vs Java performance comparisons.
/// 
/// ## Are We Fast Yet Benchmarks
/// Richards: Method dispatch & control flow
/// DeltaBlue: Object graph manipulation & polymorphism
/// Json: Data conversion, string/char handling, and DTO creation
/// List: Small expressions & structural traversal
/// 
/// ## Benchmark Configuration
/// - **Warmup**: 7 iterations to allow the Truffle background compiler and Graal JIT to reach peak steady-state.
/// - **Measurement**: 5 iterations to capture statistical averages once peak performance is reached.
/// - **Forks**: Increased to 3 to ensure result reproducibility and account for JVM startup variance.
/// 
/// This ensures isolated JVM forks and proper warmup for both the Host JIT and the Jolk interpreter/compiler.
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 7, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
/// @Fork(value = 1) is for development, but for final performance reporting, 
/// increase this to 3 or 5 to account for OS-level jitter and JVM start-up variance.
@Fork(value = 1, jvmArgsAppend = {"-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI"})
public class RunAwfjBenchmark {

    /// ## main
    /// 
    /// Entry point to execute the benchmark directly from an IDE or debugger.
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(RunAwfjBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    private JolkRuntime jolk;
    private Value jolkTest;

    @Param({"7"})
    public long n;

    @Param({"100000"})
    public int iterations;

    @Setup(Level.Trial)
    public void setup() {
        //this.jolk = new JolkRuntime();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        //this.jolk.close();
    }

    @Benchmark
    public void javaList() {
        new List().innerBenchmarkLoop(iterations);
    }

    /*
    @Benchmark
    public void jolkList() {
    }

    @Benchmark
    public void javaRichards() {
    }

    @Benchmark
    public void jolkRichards() {
    }

    @Benchmark
    public void javaDeltaBlue() {
    }

    @Benchmark
    public void jolkDeltaBlue() {
    }

    @Benchmark
    public void javaJson() {
    }

    @Benchmark
    public void jolkJson() {
    }
    */
   
}