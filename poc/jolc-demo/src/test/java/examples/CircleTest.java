package examples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import util.JolkTestBase;

/// # CircleTest
///
/// Verifies the behavior of the `Circle` archetype and its geometric calculations
/// as orchestrated by the `CircleExample`.
/// 
public class CircleTest  extends JolkTestBase {

    @BeforeEach
    public void setUp() {
        super.setUp();
        load("/examples/Circle.jolk");
        super.setUp("/examples/CircleTest.jolk");
    }

    @Test
    void testNew() {
        test("testNew");
    }

    @Test
    void testCircleArea() {
        test("testCircleArea");
    }

    @Test
    void testCircumference() {
        test("testCircumference");
    }
    
    @Test
    void testToString() {
        test("testToString");
    }
    
    @Test
    void testScaling() {
        test("testScaling");
    }
    
    @Test
    void testEquivalence() {
        test("testEquivalence");
    }
    
    @Test
    void testInvalidConstruction() {
        test("testInvalidConstruction");
    }
}
