package de.nilsbeck.jpalns;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class SenseTest {
    private static final double PRECISION = 1e-5;
    private static final double TEMPERATURE = 1.0;

    @Test
    void maximizeIsBetter() {
        assertTrue(Sense.MAXIMIZE.isBetter(10.0, 5.0, PRECISION));
        assertFalse(Sense.MAXIMIZE.isBetter(5.0, 10.0, PRECISION));
        assertFalse(Sense.MAXIMIZE.isBetter(10.0, 10.0, PRECISION));
        assertFalse(Sense.MAXIMIZE.isBetter(10.0, 10.0 + PRECISION/2, PRECISION));
        assertTrue(Sense.MAXIMIZE.isBetter(10.0 + PRECISION*2, 10.0, PRECISION));
    }

    @Test
    void minimizeIsBetter() {
        assertTrue(Sense.MINIMIZE.isBetter(5.0, 10.0, PRECISION));
        assertFalse(Sense.MINIMIZE.isBetter(10.0, 5.0, PRECISION));
        assertFalse(Sense.MINIMIZE.isBetter(10.0, 10.0, PRECISION));
        assertFalse(Sense.MINIMIZE.isBetter(10.0, 10.0 - PRECISION/2, PRECISION));
        assertTrue(Sense.MINIMIZE.isBetter(10.0 - PRECISION*2, 10.0, PRECISION));
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 5.0, 148.4131591025766",  // exp(5.0)
        "5.0, 10.0, 0.006737947",        // exp(-5.0)
        "10.0, 10.0, 1.0",               // exp(0.0)
        "0.0, 0.0, 1.0",                 // exp(0.0)
        "-10.0, -5.0, 0.006737947"       // exp(-5.0)
    })
    void maximizeAcceptanceProbability(double newValue, double currentValue, double expected) {
        assertEquals(expected, Sense.MAXIMIZE.getAcceptanceProbability(newValue, currentValue, TEMPERATURE), PRECISION);
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 5.0, 0.006737947",        // exp(-5.0)
        "5.0, 10.0, 148.4131591025766",  // exp(5.0)
        "10.0, 10.0, 1.0",               // exp(0.0)
        "0.0, 0.0, 1.0",                 // exp(0.0)
        "-10.0, -5.0, 148.4131591025766" // exp(5.0)
    })
    void minimizeAcceptanceProbability(double newValue, double currentValue, double expected) {
        assertEquals(expected, Sense.MINIMIZE.getAcceptanceProbability(newValue, currentValue, TEMPERATURE), PRECISION);
    }

    @Test
    void temperatureEffects() {
        double highTemp = 10.0;
        double lowTemp = 0.1;
        
        // With high temperature, probability should be higher for worse solutions
        double highTempProb = Sense.MAXIMIZE.getAcceptanceProbability(5.0, 10.0, highTemp);
        assertTrue(highTempProb > 0.5, "High temperature should increase acceptance probability");
        
        // With low temperature, probability should be lower for worse solutions
        double lowTempProb = Sense.MAXIMIZE.getAcceptanceProbability(5.0, 10.0, lowTemp);
        assertTrue(lowTempProb < 0.1, "Low temperature should decrease acceptance probability");
    }

    @Test
    void edgeCases() {
        // Test with very small temperature (should still work, just with very low probability)
        double smallTempProb = Sense.MAXIMIZE.getAcceptanceProbability(10.0, 5.0, Double.MIN_VALUE);
        assertTrue(smallTempProb > 0, "Should handle very small temperature");
        
        // Test with very large values
        assertTrue(Sense.MAXIMIZE.getAcceptanceProbability(Double.MAX_VALUE, Double.MAX_VALUE/2, TEMPERATURE) > 0);
        
        // Test with very small values
        assertTrue(Sense.MINIMIZE.getAcceptanceProbability(Double.MIN_VALUE, Double.MIN_VALUE*2, TEMPERATURE) > 0);
    }
} 
