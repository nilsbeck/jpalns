package de.nilsbeck.jpalns;

/**
 * Enum representing the different weight selection options.
 */
enum WeightSelection
{
    Accepted(1),
    BetterThanCurrent(2),
    NewGlobalBest(3),
    Rejected(0);

    private int value ;
    WeightSelection(int value) {
        this.value = value;
    }

    public int getValue(){
        return this.value;
    }
}
