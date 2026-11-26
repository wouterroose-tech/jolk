package runner;

import awfj.Run;

public class AwfjRunner {

  public static void main(String[] args) throws Exception {
      Run listBecnhmark = new Run("List", 3, 7, 10000);
      listBecnhmark.runBenchmark();
      listBecnhmark.printTotal();
  }

}
