package com.realmone.tleasy.tle;

import com.realmone.tleasy.TleFilter;
import lombok.Builder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * This simple implementation of the {@link TleFilter} interface allows for the filtering of TLE data based on specific
 * targeted NORAD Identifiers within the data.  Labels/comments are excluded, and only rows that contain the specified
 * identifiers are included in the resulting output.
 */
public class SimpleTleFilter implements TleFilter {

    /**
     * The {@link Set} of NORAD Identifiers to target from the incoming stream for our output.
     */
    private final Set<String> targetNoradIds;

    /**
     * Lombok generated builder based on this private constructor.
     *
     * @param targetNoradIds The NORAD Identifiers you want to include in the output
     */
    @Builder
    private SimpleTleFilter(Set<String> targetNoradIds) {
        this.targetNoradIds = targetNoradIds;
    }

    /**
     * Implementation of the filter method that will only include TLE data that specifies a specific set of NORAD
     * identifiers.
     *
     * @param input  The incoming TLE data
     * @param output The TLE data to include in the output of the filter
     * @throws IOException If there is an issue processing the stream of TLE data
     */
    @Override
    public long filter(InputStream input, OutputStream output) throws IOException {
        long counter = 0L;
        // Handle the buffered reader/writer in the try block to ensure they're closed appropriately
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            // Optional title line
            String title = null;
            // Required Two-Line Element lines :)
            String line1;
            String line2;
            // Read the buffered input one TLE entry at a time (2-3 lines)
            while ((line1 = reader.readLine()) != null) {
                // If line1 is not a TLE line (i.e., doesn't start with "1 "), treat it as a title
                if (!line1.startsWith("1 ")) {
                    title = line1;
                    line1 = reader.readLine(); // The actual first TLE line
                }
                // The second TLE line
                line2 = reader.readLine();
                // Ensure both lines are valid
                if (line1 == null || line2 == null || !line1.startsWith("1 ") || !line2.startsWith("2 ")) {
                    throw new IOException("TLE data was malformed");
                }
                // Extract the NORAD ID from the second TLE line (bypass the need for the classification marking handling)
                String noradId = extractNoradId(line2);
                // If NORAD ID matches, write it to the output
                if (targetNoradIds.contains(noradId)) {
                    if (title != null) {
                        writer.write(title);
                        writer.newLine();
                    }
                    writer.write(line1);
                    writer.newLine();
                    writer.write(line2);
                    writer.newLine();
                    counter++;
                }
                title = null; // Reset title for the next entry
            }

            writer.flush(); // Ensure everything is written to the output
        }
        return counter;
    }

    private String extractNoradId(String line2) {
        String[] parts = line2.split("\\s+", 3);
        return (parts.length > 1) ? parts[1] : "";
    }
}
