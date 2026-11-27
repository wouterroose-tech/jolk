package runner.basic;

import runner.JolkRuntime;

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
