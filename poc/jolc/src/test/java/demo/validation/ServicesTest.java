package demo.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
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

    @Test
    @Disabled("activate when Record is implemented")
    void testCity() {
        Value city = this.cityClass().invokeMember("new", 12345, "New York", "NY");
        //assertEquals(12345L, city.invokeMember("zipcode"));
        assertEquals("New York", city.invokeMember("name"));
        assertEquals("NY", city.invokeMember("province"));
    }

    @Test
    @Disabled("activate when Array is implemented")
    void testGeoGraphicalService() {
        Value service = this.geoGraphicalServiceClass().invokeMember("new");
    }
}
