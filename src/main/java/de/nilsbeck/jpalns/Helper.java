package de.nilsbeck.jpalns;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class for various utility methods.
 */
public class Helper {

    /**
     * Converts a list of doubles to a cumulative enumerable.
     * @param input the input list of doubles
     * @return the cumulative enumerable
     */
    public static List<Double> toCumulativeEnumerable(List<Double> input) {
        if (input == null || input.size() < 1)
            return null;
        double temp = 0.0;
        double sum = input.stream().mapToDouble(number -> number).sum();

        ArrayList<Double> output = new ArrayList<>();
        for (Double number:input.stream().collect(Collectors.toList())) {
            temp+=number;
            output.add(temp/sum);
        }
        return output;
    }
}
