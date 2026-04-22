package tolk.perf;

import org.graalvm.polyglot.Value;

public class BasePerformanceTest extends BasicPerformanceTest {

    public static void main(String[] args) {
        new BasePerformanceTest().run(args);
    }
    

    public void test() throws InterruptedException {
        
        Value jolkTest = getJolkTest();

        System.out.println("run  iterations   runBase  runString    runNumerical runFibonacci");
        System.out.println("----|------------|------------|------------|------------|------------");

        int it = 1;
        for (int i = 1; i <= 64; i++) {
            long tRun = measureJolk(jolkTest, "runBase", 7, it);
            System.gc(); Thread.sleep(100); 
            long tString = measureJolk(jolkTest, "runString", 1024, it);
            System.gc(); Thread.sleep(100); 
            long tNumerical = measureJolk(jolkTest, "runNumerical", 1024, it);
            System.gc(); Thread.sleep(100); 
            long tFib = measureJolk(jolkTest, "runFibonacci", 7, it);

            System.out.printf("%4d|%12d|%12d|%12d|%12d|%12d\n", i, it, tRun / it, tString / it, tNumerical / it, tFib / it);
            it = Math.min(4096, it *2);
            System.gc(); Thread.sleep(100); 
        }
        System.out.println();
    }

}
