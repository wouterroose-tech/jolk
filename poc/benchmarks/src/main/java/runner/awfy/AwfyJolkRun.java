package runner.awfy;

import java.util.function.Supplier;

import org.graalvm.polyglot.Value;

import runner.JolkRuntime;

public final class AwfyJolkRun extends AwfyRun<Value> {

  public static void main(String[] args) throws Exception {
    new AwfyJolkRun(args[0]).run();
  }

  private final JolkRuntime jolk;

  public AwfyJolkRun(final String name) {
    super(name);
    this.jolk = new JolkRuntime();
    jolk.loadClass("/awfy/Benchmark.jolk");
    jolk.loadClass("/awfy/Element.jolk");
  }

  protected Supplier<Value> getSuiteFromName(final String name) {
    switch (name) {
      //case "Bounce":      return () -> new Bounce();
      //case "CD":          return () -> new CD();
      //case "DeltaBlue":   return () -> new DeltaBlue();
      //case "Havlak":      return () -> new Havlak();
      //case "Json":        return () -> new Json();
      case "List":        return () -> jolk.createInstance("/awfy/List.jolk");
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

  @Override  protected boolean innerBenchmarkLoop(final Value bench, int innerIterations) {
    return bench.invokeMember("innerBenchmarkLoop",  innerIterations).asBoolean();
  }
  
}
