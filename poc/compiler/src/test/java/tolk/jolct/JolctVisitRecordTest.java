package tolk.jolct;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JolctVisitRecordTest extends JolctVisitorTest {

    @Test
    public void testSimpleRecord() {
        String source = "record Point { Int x; Int y; }";
        String expected = "public record Point (Int x, Int y) {\n}\n";
        assertFullTranspilation(expected, source);
    }

    @Test
    public void testRecordWithRecordField() {
        // Ensures that 'Person' is not transpiled as 'Person<?>' inside the ContactForm record
        String source = "record ContactForm { Person person; }";
        String expected = "public record ContactForm (Person person) {\n}\n";
        JolkContext context = new JolkContext();
        context.addJolkType("Person");
        assertEquals(expected.replace("\r\n", "\n").trim(), transpile(source, context).replace("\r\n", "\n").trim());
    }
}