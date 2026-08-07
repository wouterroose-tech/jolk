package jolk.test.engine.bridge;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.nio.file.Path;

public class JolkLauncherBridgeExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // 1. Target the source directory containing .jolk files
        DiscoverySelector directorySelector = DiscoverySelectors.selectDirectory(
            Path.of("src/test/jolk").toAbsolutePath().toString()
        );

        // 2. Build the discovery request explicitly targeting the Jolk engine
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(directorySelector)
                .configurationParameter("junit.platform.engine.jolk.enabled", "true")
                .build();

        // 3. Obtain a Launcher instance from LauncherFactory
        Launcher launcher = LauncherFactory.create();

        // 4. Register a custom listener to capture execution lifecycle events
        TestExecutionListener executionListener = new JolkExecutionReportingListener();
        launcher.registerTestExecutionListeners(executionListener);

        // 5. Discover the TestPlan directly
        TestPlan testPlan = launcher.discover(request);

        // 6. Execute the discovered test plan
        if (testPlan.containsTests()) {
            launcher.execute(testPlan);
        }
    }
}