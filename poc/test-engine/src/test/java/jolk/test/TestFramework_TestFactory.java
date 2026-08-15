package jolk.test;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;


// Disabled, test now run via the JolkTestEngine mvn integration
@Disabled
public class TestFramework_TestFactory extends jolk.test.TestFactory {

    @TestFactory
    Stream<DynamicNode> discoverJolkTests() {
        return discoverJolkTests("src/test/jolk");
    }

}
