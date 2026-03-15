package tolk.language;

import org.graalvm.polyglot.Language;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkLanguageTest
 *
 * Verifies the registration and configuration of the Jolk language within the
 * GraalVM Polyglot Engine.
 */
public class JolkLanguageTest extends JolcTestBase {

    @Test
    void testLanguageIsRegistered() {
        assertTrue(engine.getLanguages().containsKey(JolkLanguage.ID),
                "The Jolk language should be registered in the GraalVM Engine.");
    }

    @Test
    void testLanguageMetadata() {
        Language language = engine.getLanguages().get(JolkLanguage.ID);
        assertNotNull(language, "The language instance should not be null.");
        
        assertEquals(JolkLanguage.ID, language.getId(), "Language ID should match the constant.");
        assertFalse(language.getName().isEmpty(), "Language name should be defined.");
        assertFalse(language.getMimeTypes().isEmpty(), "Language should define at least one MIME type.");
    }

}
