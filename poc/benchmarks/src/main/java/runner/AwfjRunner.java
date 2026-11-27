package runner;

import awfj.Run;

public class AwfjRunner {

  private static int WARMUP_ITERATIONS = 3;
  private static int NUM_ITERATIONS = 7;
  private static int INNER_ITERATIONS = 10000;

  public static void main(String[] args) throws Exception {
      Run listBenchmark = getBenchmark("List");
      run(listBenchmark, WARMUP_ITERATIONS);
      
      listBenchmark = getBenchmark("List");
      run(listBenchmark, NUM_ITERATIONS);
      listBenchmark.printTotal();
  }

  private static void run(Run benchmark, int iterations) {
    benchmark.setNumIterations(iterations); 
    benchmark.runBenchmark();
  }

  private static Run getBenchmark(String benchmark) {
    Run run = new Run(benchmark);
    run.setInnerIterations(INNER_ITERATIONS);
    return run;
  }

}
