package demo.validation;

import org.graalvm.polyglot.Value;

import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;


/// This test class is for testing service classes of the Jolk Demo.
/// test parsing, creation and protocol

public class ServicesTest extends JolcTestBase {

    private Value geoGraphicalServiceClass() {
        String source = """
            final class GeoGraphicalService {
                
                public meta constant Array<Int> MECHELEN = #[2800, 2801, 2811, 2812];

                // Caching the external service
                public meta stable GeoGraphicalService GGS = GeoGraphicalService #new;

                Boolean exists(Long zipCode) {
                    // simulate lookup via DAO
                    ^ 1000 < zipCode && zipCode < 9999
                }
            }""";
        return eval(source);
    }

    @Test
    private Value cityClass() {
        String source = """
            record City {
                Int zipcode;
                String name;
                String province;
            }""";
        return eval(source);
    }

}
