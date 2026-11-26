package awfj;

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
public final class Run {
  private final String name;
  private final Supplier<Benchmark> benchmarkSuite;
  private int warmupIterations;
  private int numIterations;
  private int innerIterations;
  private long total;

  public Run(final String name, int warmupIterations, int numIterations, int innerIterations) {
    this.name = name;
    this.benchmarkSuite = getSuiteFromName(name);
    this.warmupIterations = warmupIterations;
    this.numIterations = numIterations;
    this.innerIterations = innerIterations;
  }

  private static Supplier<Benchmark> getSuiteFromName(final String name) {
    switch (name) {
      //case "Bounce":      return () -> new Bounce();
      //case "CD":          return () -> new CD();
      //case "DeltaBlue":   return () -> new DeltaBlue();
      //case "Havlak":      return () -> new Havlak();
      //case "Json":        return () -> new Json();
      case "List":        return () -> new List();
      //case "Mandelbrot":  return () -> new Mandelbrot();
      //case "NBody":       return () -> new NBody();
      //case "Permute":     return () -> new Permute();
      //case "Queens":      return () -> new Queens();
      //case "Richards":    return () -> new Richards();
      //case "Sieve":       return () -> new Sieve();
      //case "Storage":     return () -> new Storage();
      //case "Towers":      return () -> new Towers();
      default:
        throw new RuntimeException("No benchmark found with the name: " + name);
    }
  }

  public void runBenchmark() {
    
    System.out.println("Warming up " + name + " benchmark ...");
    warmup(benchmarkSuite.get());

    // Checkstyle: stop
    System.out.println("Starting " + name + " benchmark ...");
    // Checkstyle: resume

    doRuns(benchmarkSuite.get());
    reportBenchmark();

    // Checkstyle: stop
    System.out.println();
    // Checkstyle: resume
  }

  private void measure(final Benchmark bench, final int iteration) {
    long startTime = System.nanoTime();
    if (!bench.innerBenchmarkLoop(innerIterations)) {
      throw new RuntimeException("Benchmark failed with incorrect result");
    }
    long endTime = System.nanoTime();
    long runTime = (endTime - startTime) / 1000;

    printResult(runTime, iteration);

    total += runTime;
  }

  private void warmup(final Benchmark bench) {
    for (int i = 1; i <= warmupIterations; i++) {
      measure(bench, i);
    }
  }

  private void doRuns(final Benchmark bench) {
    for (int i = 1; i <= numIterations; i++) {
      measure(bench, i);
    }
  }

  private void reportBenchmark() {
    // Checkstyle: stop
    System.out.println(name + ": iterations=" + numIterations +
        " average: " + (total / numIterations) + "us total: " + total + "us\n");
    // Checkstyle: resume
  }

  private void printResult(final long runTime, final int iterations) {
    // Checkstyle: stop
    System.out.println(name + ": iteration=" + iterations + " runtime: " + runTime + "us");
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
}
