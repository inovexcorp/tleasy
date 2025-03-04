package com.tleasy.tle;

import lombok.Builder;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class TleFilterPredicate implements Predicate<String> {

    private static final Pattern PATTERN_TLE_LINE = Pattern.compile("^[12]\\s+"); // Matches lines starting with "1" or "2"
    private final Set<String> targetNoradIdSet; // The set of NORAD IDs to match

    @Builder
    private TleFilterPredicate(Set<String> targetIds) {
        targetNoradIdSet = new HashSet<>(targetIds.size() * 2);
        // Add the specified target identifiers
        targetNoradIdSet.addAll(targetIds);
        // Add the U suffix to all targets to ensure they are included :)
        targetNoradIdSet.addAll(targetIds.stream().map(str -> String.format("%sU", str)).toList());

    }

    @Override
    public boolean test(String line) {
        // Ensure the line follows the expected TLE format (ignoring labels/comments)
        if (!PATTERN_TLE_LINE.matcher(line).find()) {
            return false;
        }
        // Split by whitespace and check if the second column (NORAD ID) exists
        final String[] parts = line.split("\\s+", 3);
        return parts.length > 1 && targetNoradIdSet.contains(parts[1]); // Compare against target IDs
    }
}