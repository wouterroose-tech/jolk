package runner.basic;

import org.graalvm.polyglot.Value;

import runner.JolkRuntime;

public class RunBasicJolkBenchmark extends BaseRunner {

    public static void main(String[] args) {
        new RunBasicJolkBenchmark().run(args);
    }

    @Override 
    protected void run(JolkRuntime jolk) throws Throwable {
        
        Value jolkTest = jolk.createInstance("/basic/BasicBenchmark.jolk");

        System.out.println("run  iterations   base         String       Numerical    Fibonacci_i    Fibonacci_r");
        System.out.println("----|------------|------------|------------|------------|------------|------------");

        long it = 1; // Increased to 10M to ensure the sampler can catch the tight loops
        for (int i = 1; i <= 32; i++) {
            long tRun = measureJolk(jolkTest, "runBase", 7, it);
            long tString = measureJolk(jolkTest, "runString", 1024, it);
            long tNumerical = measureJolk(jolkTest, "runNumerical", 1024, it);
            long tFib_i = measureJolk(jolkTest, "runFibonacciIterative", 7, it / 10);
            long tFib_r = measureJolk(jolkTest, "runFibonacci", 7, it / 10);

            System.out.printf("%4d|%12d|%12d|%12d|%12d|%12d|%12d\n", i, it, tRun / it, tString / it, tNumerical / it, (tFib_i * 10) / it, (tFib_r * 10) / it);
            it = Math.min(128 * 1024 , it * 2); 
            //System.gc(); Thread.sleep(100); 
        }
        System.out.println();
    }

    private long measureJolk(Value jolkTest, String member, long n, long iterations) {
        long start = System.nanoTime();
        jolkTest.invokeMember(member, n, iterations);
        return System.nanoTime() - start;
    }

}
