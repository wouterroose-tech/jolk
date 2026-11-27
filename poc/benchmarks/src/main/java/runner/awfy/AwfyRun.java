package runner.awfy;

import java.util.function.Supplier;

/* This code is based on the SOM class library.
 *
 * Copyright (c) 2001-2016 see AUTHORS.md file
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the 'Software'), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public abstract class AwfyRun<T> {

  private static int WARMUP_ITERATIONS = 3;
  private static int NUM_ITERATIONS = 7;
  private static int INNER_ITERATIONS = 1000;

  private final String name;
  private final Supplier<T> benchmarkSuite;
  private int numIterations;
  private int innerIterations;
  private long total;

  public AwfyRun(final String name) {
    this.name = name;
    this.benchmarkSuite = getSuiteFromName(name);
    this.innerIterations = INNER_ITERATIONS;
  }
  
  protected final void run() throws Exception {
    System.out.println("Warump ...");
    run(WARMUP_ITERATIONS);
    
    total = 0;
    System.out.println("Run  ...");
    run(NUM_ITERATIONS);
  }

  public void run(final int runs) throws Exception {
    setNumIterations(runs); 
    runBenchmark();
  }
  

  protected abstract Supplier<T> getSuiteFromName(String name2);

  public void runBenchmark() {
    // Checkstyle: stop
    System.out.println("Starting " + name + " benchmark ...");
    // Checkstyle: resume

    doRuns(benchmarkSuite.get());
    reportBenchmark();

    // Checkstyle: stop
    System.out.println();
    // Checkstyle: resume
  }

  private void measure(final T bench) {
    long startTime = System.nanoTime();
    if (!innerBenchmarkLoop(bench,innerIterations)) {
      throw new RuntimeException("Benchmark failed with incorrect result");
    }
    long endTime = System.nanoTime();
    long runTime = (endTime - startTime) / 1000;

    printResult(runTime);

    total += runTime;
  }

  protected abstract boolean innerBenchmarkLoop(final T bench, final int innerIterations);

  private void doRuns(final T bench) {
    for (int i = 0; i < numIterations; i++) {
      measure(bench);
    }
  }

  private void reportBenchmark() {
    // Checkstyle: stop
    System.out.println(name + ": iterations=" + numIterations +
        " average: " + (total / numIterations) + "us total: " + total + "us\n");
    // Checkstyle: resume
  }

  private void printResult(final long runTime) {
    // Checkstyle: stop
    System.out.println(name + ": iterations=1 runtime: " + runTime + "us");
    // Checkstyle: resume
  }

  public void printTotal() {
    // Checkstyle: stop
    System.out.println("Total Runtime: " + total + "us");
    // Checkstyle: resume
  }

  public void setNumIterations(final int numIterations) {
    this.numIterations = numIterations;
  }

  public void setInnerIterations(final int innerIterations) {
    this.innerIterations = innerIterations;
  }

  public long total() {
    return total;
  }
}
