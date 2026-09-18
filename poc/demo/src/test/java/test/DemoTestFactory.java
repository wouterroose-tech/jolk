package test;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;


// Disabled, test now run via the JolkTestEngine mvn integration
@Disabled
public class DemoTestFactory extends jolk.test.TestFactory {

    @TestFactory
    Stream<DynamicNode> discoverJolkTests() {
        // load jolk classes
        discoverJolkTests("src/main/jolk/examples");
        discoverJolkTests("src/main/jolk/demonstrators");

        // issue with SsnConstraintTest test_accept_failure => class not loaded ???
        discoverJolkTests("src/main/jolk/demo");
        // load jolk test classes
        return discoverJolkTests("src/test/jolk");
    }

}
