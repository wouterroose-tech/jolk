package tolk.perf;

///
/// A simple performance test to count uppercase characters in a string. 
/// This test is designed to be run with the Graal JIT compiler enabled, 
/// and it includes timing information to observe the performance characteristics of the code as it executes.
/// 
/// source: https://www.baeldung.com/graal-java-jit-compiler
/// 
/// set these argumens to activate GraalJIT
///    vmArgs:
///     -XX:+UnlockExperimentalVMOptions,
///     -XX:+UnlockExperimentalVMOptions",
///     -XX:+EnableJVMCI,
///     -XX:+UseJVMCICompiler 
/// 
public class CountUppercase {
    static final int ITERATIONS = Math.max(Integer.getInteger("iterations", 1), 1);

    public static void main(String[] args) {
        String sentence = "TestingTheGraalJITCompilerWithSomeUPPERCASE";
        System.out.println("-- iteration --");
        for (int iter = 0; iter < ITERATIONS; iter++) {
            if (ITERATIONS != 1) {
                System.out.println();
            }
            long total = 0, start = System.currentTimeMillis(), last = start;
            for (int i = 1; i < 10_000_000; i++) {
                total += sentence
                  .chars()
                  .filter(Character::isUpperCase)
                  .count();
                if (i % 1_000_000 == 0) {
                    long now = System.currentTimeMillis();
                    System.out.printf("%d (%d ms)%n", i / 1_000_000, now - last);
                    last = now;
                }
            }
            System.out.printf("total: %d (%d ms)%n", total, System.currentTimeMillis() - start);
        }
    }
}
