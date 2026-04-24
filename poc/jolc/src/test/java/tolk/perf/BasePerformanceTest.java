package tolk.perf;

import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

public class BasePerformanceTest extends BasicPerformanceTest {

    public static void main(String[] args) {
        new BasePerformanceTest().run(args);
    }

    @Override
    protected Engine.Builder getEngine() {
        return super.getEngine()
                .out(System.out)
                .err(System.err)
                //.option("cpusampler", "true")
                //.option("cpusampler.ShowInternal", "true")
                //.option("cpusampler.ShowTiers", "true")
                //.option("cpusampler.Period", "1")
                //.option("cpusampler.StackLimit", "100")
                //.option("cpusampler.SampleContextInitialization", "true");
                /*
                [engine] WARNING: The polyglot engine uses a fallback runtime that does not support 
                runtime compilation to native code.
                Execution without runtime compilation will negatively impact the guest application performance.
                'jolc' is imported by Maven, changes made to the classpath might be lost after reloading. 
                >>>
                To make permanent changes, please edit the pom.xml file.
                 */
                ;
    }
    

    public void test() throws InterruptedException {
        
        Value jolkTest = getJolkTest();

        System.out.println("run  iterations   runBase      runString    runNumerical runFibonacci");
        System.out.println("----|------------|------------|------------|------------|------------");

        long it = 1; // Increased to 10M to ensure the sampler can catch the tight loops
        for (int i = 1; i <= 64; i++) {
            long tRun = measureJolk(jolkTest, "runBase", 7, it);
            long tString = measureJolk(jolkTest, "runString", 1024, it);
            long tNumerical = measureJolk(jolkTest, "runNumerical", 1024, it);
            // Fibonacci is recursive and naturally slower; 1M is enough for it
            long tFib = measureJolk(jolkTest, "runFibonacci", 7, it / 10);

            System.out.printf("%4d|%12d|%12d|%12d|%12d|%12d\n", i, it, tRun / it, tString / it, tNumerical / it, (tFib * 10) / it);
            it = Math.min(128 * 1024 , it * 2); 
            //System.gc(); Thread.sleep(100); 
        }
        System.out.println();
    }

}
