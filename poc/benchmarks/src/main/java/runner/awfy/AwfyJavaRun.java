package runner.awfy;

import java.util.function.Supplier;

import awfy.Benchmark;
import awfy.List;

public final class AwfyJavaRun extends AwfyRun<Benchmark> {

  public static void main(String[] args) throws Exception {
    new AwfyJavaRun(args[0]).run();
  }

  public AwfyJavaRun(final String name) {
    super(name);
  }

  protected Supplier<Benchmark> getSuiteFromName(final String name) {
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

  @Override
  protected boolean innerBenchmarkLoop(final Benchmark bench, int innerIterations) {
    return bench.innerBenchmarkLoop(innerIterations);
  }
  
}
