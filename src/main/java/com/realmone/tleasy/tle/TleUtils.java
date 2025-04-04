package com.realmone.tleasy.tle;

import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class TleUtils {

    /**
     * Parses the text from the ID input field to extract valid ID numbers.
     *
     * <p>
     * This method supports two input formats:
     * <ul>
     *   <li>Single 5-digit identifiers (e.g., "12345").</li>
     *   <li>Ranges of 5-digit identifiers specified with a hyphen (e.g., "12345-12350").
     *       In this case, all numbers in the inclusive range are added to the result.</li>
     * </ul>
     * The input string may contain multiple IDs or ranges separated by commas. Whitespace around
     * commas or hyphens is ignored. If a range is specified where the start number is greater than
     * the end number, an {@code IllegalArgumentException} is thrown.
     * </p>
     *
     * @return a {@link Set} of ID Strings extracted from the input; returns an empty set if no valid IDs
     * are found.
     */
    public static Set<String> parseIdentifiers(String inputString) {
        String input = inputString.trim();
        Set<String> result = new HashSet<>();
        // Split input by commas to allow mixed single IDs and ranges
        String[] parts = input.split("\\s*,\\s*");
        for (String part : parts) {
            // If the part is a 5 digit identifier
            if (part.matches("\\d{5}")) {
                result.add(part);
            }
            // Else if the part is a range specification
            else if (part.matches("\\d{5}\\s*-\\s*\\d{5}")) {
                // Grab the start and end part
                String[] rangeParts = part.split("\\s*-\\s*");
                int start = Integer.parseInt(rangeParts[0]);
                int end = Integer.parseInt(rangeParts[1]);
                // Simple validation to ensure the first number is smaller than the second...
                if (start > end) {
                    throw new IllegalArgumentException("Range must be in ascending order.");
                }
                // Add all the target numbers in the specified range
                result.addAll(getNumbersBetween(start, end).stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet()));
            }
        }
        return result;
    }

    /**
     * Creates a Set of integers representing the range of numbers between the provided start and end values.
     *
     * @param start The integer to start the range with
     * @param end   The integer to end the range with
     * @return A {@link Set} of {@link Integer}s representing all numbers from the start to the end inclusive
     */
    public static Set<Integer> getNumbersBetween(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("Start number must be less than or equal to end number.");
        } else {
            Set<Integer> numbers = new HashSet<>();
            for (int i = start; i <= end; i++) {
                numbers.add(i);
            }
            return numbers;
        }
    }
}
