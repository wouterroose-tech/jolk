package jolk.test;

import jolk.test.engine.EmptyConfigurationParameters;
import jolk.test.engine.JolkTestEngine;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.FileSource;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.opentest4j.TestAbortedException;
import org.opentest4j.TestSkippedException;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


///
/// # JolkTestRunner
/// 
/// Temporary: The static Java anchor for VS Code.
/// 
/// @author Wouter Roose
/// 
// Disabled, test now run via the JolkTestEngine for the JUnit test framework
@Disabled
public class TestFactoryRunner {

    private final JolkTestEngine engine = new JolkTestEngine();

    @TestFactory
    Stream<DynamicNode> discoverJolkTests() {
        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectDirectory(Path.of("src/test/jolk").toFile()))
                .build();

        UniqueId baseId = UniqueId.forEngine(engine.getId());
        TestDescriptor rootDescriptor = engine.discover(request, baseId);

        // Convert root descriptor children into top-level dynamic nodes
        List<DynamicNode> nodes = new ArrayList<>();
        for (TestDescriptor child : rootDescriptor.getChildren()) {
            nodes.add(convertToDynamicNode(child));
        }

        return nodes.stream();
    }

    private DynamicNode convertToDynamicNode(TestDescriptor descriptor) {
        URI sourceUri = extractUri(descriptor).orElse(null);

        if (descriptor.isContainer()) {
            // Group children under a DynamicContainer (Package or Class node)
            List<DynamicNode> children = new ArrayList<>();
            for (TestDescriptor child : descriptor.getChildren()) {
                children.add(convertToDynamicNode(child));
            }

            return DynamicContainer.dynamicContainer(
                    descriptor.getDisplayName(),
                    sourceUri,
                    children.stream()
            );
        } else {
            // Leaf Test Node (Jolk selector / test method)
            return DynamicTest.dynamicTest(
                    descriptor.getDisplayName(),
                    sourceUri,
                    () -> executeSingleNode(descriptor)
                );
        }
    }

    private Optional<URI> extractUri(TestDescriptor descriptor) {
        return descriptor.getSource()
                .filter(src -> src instanceof FileSource)
                .map(src -> ((FileSource) src).getUri());
    }

    private void executeSingleNode(TestDescriptor targetNode) throws Throwable {

        // Capture execution failures reported by the engine
        final List<Throwable> failures = new ArrayList<>();
        final String targetId = targetNode.getUniqueId().toString();

        EngineExecutionListener bridgeListener = new EngineExecutionListener() {
            @Override
            public void executionStarted(TestDescriptor testDescriptor) {
                // Optional trace logging
            }

            @Override
            public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
                if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
                    testExecutionResult.getThrowable().ifPresent(failures::add);
                }
            }
            @Override
            public void executionSkipped(TestDescriptor testDescriptor, String reason) {
                String currentId = testDescriptor.getUniqueId().toString();
                // Check if the event belongs to our target node or its descendants
                if (currentId.equals(targetId) || currentId.startsWith(targetId)) {
                    failures.add(new TestAbortedException("Execution skipped: " + reason));
                }
            }
        };

        // Construct the ExecutionRequest targeting the single node
        ExecutionRequest request = new ExecutionRequest(
                targetNode,
                bridgeListener,
                EmptyConfigurationParameters.INSTANCE
        );

        // Delegate execution directly to JolkTestEngine
        //engine.execute(request);
        try {
            engine.execute(request);
        } catch (Throwable t) {
            // Direct execution engine panic / unexpected exception
            failures.add(t);
        }

        // Rethrow any captured exception to signal failure to JUnit Jupiter/VS Code
        if (!failures.isEmpty()) {Throwable failure = failures.get(0);
            // Map TestSkippedException to TestAbortedException at the bridge boundary
            if (failure instanceof TestSkippedException skipped) {
                throw new TestAbortedException(skipped.getMessage(), skipped);
            }
            throw failure;
        }
    }

}