package com.tleasy.tle;

import com.tleasy.TleFilter;
import lombok.Builder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.function.Predicate;

public class SimpleTleFilter implements TleFilter {

    private final Predicate<String> filterCondition;

    @Builder
    private SimpleTleFilter(Set<String> targetNoradIds) {
        filterCondition = TleFilterPredicate.builder()
                .targetIds(targetNoradIds)
                .build();
    }

    @Override
    public void filter(InputStream tleStream, OutputStream outputStream) throws IOException {
        try (
                // BufferedReader for handling the reading of incoming bytes
                BufferedReader reader = new BufferedReader(new InputStreamReader(tleStream));
                // BufferedWriter for handling the writing of matching output bytes
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (filterCondition.test(line)) {
                    writer.write(line);
                    writer.newLine();  // Ensure line separation
                }
            }
            writer.flush(); // Ensure data is fully written to the output
        }
    }
}
