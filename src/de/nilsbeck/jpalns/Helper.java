package de.nilsbeck.jpalns;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Helper {
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
