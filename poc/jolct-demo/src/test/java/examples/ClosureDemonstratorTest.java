package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ClosureDemonstratorTest {

    private final ClosureDemonstrator demonstrator = new ClosureDemonstrator();

    @Test
    void testRunClosureParam() {
        assertEquals(List.of("aa", "bb", "cc"), demonstrator.runClosureParam());
    }

    @Test
    void testRunMethodClass() {
        assertEquals(List.of("xx", "yy", "zz"), demonstrator.runMethodReference_class());
    }

    @Test
    void testRunMethodInstance() {
        assertEquals(List.of("xx", "yy", "zz"), demonstrator.runMethodReference_instance());
    }

    @Test
    void testRunMethodReference() {
        assertEquals(List.of("xx", "yy", "zz"), demonstrator.runMethodReference_self());
    }
}
