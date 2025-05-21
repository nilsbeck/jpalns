package de.nilsbeck.jpalns;

public enum Sense {
    MAXIMIZE,
    MINIMIZE;

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
