package runner;

///
/// set these argumens to activate GraalJIT
///    vmArgs:
///     -XX:+UnlockExperimentalVMOptions,
///     -XX:+EnableJVMCI,
///     -XX:+UseJVMCICompiler 
///     -Xms4G -Xmx4G
///     -XX:MetaspaceSize=512m
///     -XX:ReservedCodeCacheSize=512m
///     -XX:+HeapDumpOnOutOfMemoryError
/// 
public abstract class BaseRunner {
    
    public void run(String[] args) {
        try (JolkRuntime jolk = new JolkRuntime()) {
            run(jolk);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected abstract void run(JolkRuntime jolk) throws Throwable;

}
