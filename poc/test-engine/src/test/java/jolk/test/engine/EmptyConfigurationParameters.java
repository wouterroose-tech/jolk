package jolk.test.engine;

import org.junit.platform.engine.ConfigurationParameters;
import java.util.Optional;
import java.util.Set;

public final class EmptyConfigurationParameters implements ConfigurationParameters {
    public static final ConfigurationParameters INSTANCE = new EmptyConfigurationParameters();

    private EmptyConfigurationParameters() {}

    @Override
    public Optional<String> get(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        return Optional.empty();
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException("Unimplemented method 'keySet'");
    }
}
