package de.nilsbeck.jpalns;

public enum Sense {
    MAXIMIZE,
    MINIMIZE;

    /**
     * Returns true if the new value is better than the current value.
     * @param newValue the new value
     * @param currentValue the current value
     * @param precision the precision of the comparison
     * @return true if the new value is better than the current value
     */
    public boolean isBetter(double newValue, double currentValue, double precision) {
        switch (this) {
            case MAXIMIZE:
                return newValue - currentValue > precision;
            case MINIMIZE:
                return currentValue - newValue > precision;
            default:
                throw new IllegalStateException("Unexpected optimization type");
        }
    }

    /**
     * Returns the acceptance probability of the new value.
     * @param newValue the new value
     * @param currentValue the current value
     * @param temperature the temperature of the system
     * @return the acceptance probability of the new value
     */
    public double getAcceptanceProbability(double newValue, double currentValue, double temperature) {
        switch (this) {
            case MAXIMIZE:
                return Math.exp((newValue - currentValue) / temperature);
            case MINIMIZE:
                return Math.exp((currentValue - newValue) / temperature);
            default:
                throw new IllegalStateException("Unexpected optimization type");
        }
    }
} 
