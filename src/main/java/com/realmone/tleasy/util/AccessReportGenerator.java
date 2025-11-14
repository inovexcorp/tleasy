package com.realmone.tleasy.util;

import com.realmone.tleasy.Configuration;
import com.realmone.tleasy.TLEasy;
import com.realmone.tleasy.tle.TleUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import com.realmone.tleasy.TLEasy.AccessReportResult;

public class AccessReportGenerator {

    private final StkCon stkConnection;
    private final TLEasy tleasyInstance; // Used to call the setStatus method

    /**
     * Constructs an AccessReportGenerator.
     * @param stkConnection The active connection to STK.
     * @param tleasyInstance The main application instance to update the UI status.
     */
    public AccessReportGenerator(StkCon stkConnection, TLEasy tleasyInstance) {
        this.stkConnection = stkConnection;
        this.tleasyInstance = tleasyInstance;
    }

    /**
     * Computes access between all loaded satellites and existing facilities, then generates a CSV report.
     * @param scenarioName The name of the target scenario.
     * @param satelliteNames A list of satellite names to calculate access for.
     * @param sanitizedTleFile The TLE file, used to check epoch dates for labeling.
     * @return An AccessReportResult object containing the CSV data and filtered access count.
     */
    public AccessReportResult generateAccessReportCsv(String scenarioName, List<String> satelliteNames, File sanitizedTleFile) throws IOException {
        tleasyInstance.setStatus("Calculating access to ground facilities...");
        String facilityListStr = stkConnection.sendConCommand("AllInstanceNames / Facility");
        if (facilityListStr.trim().isEmpty() || facilityListStr.contains("E_CommandFailed")) {
            return null; // No facilities to check against or command failed
        }

        List<String> facilityNames = new ArrayList<>();
        Pattern pattern = Pattern.compile("/Facility/([^/]+)");
        Matcher matcher = pattern.matcher(facilityListStr);
        while (matcher.find()) {
            String fullName = matcher.group(1);
            String shortenedName = fullName.replaceAll("Facility$", "");
            facilityNames.add(shortenedName);
        }

        if (facilityNames.isEmpty()) {
            System.out.println("No facilities found in the scenario.");
            return null;
        }

        final LocalDateTime accessTimeLimit = LocalDateTime.now(ZoneOffset.UTC).plusHours(24);
        final LocalDateTime julianDateLimit = LocalDateTime.now(ZoneOffset.UTC).minusDays(1);

        final boolean accessFilterEnabled = Configuration.isAccessTimeFilterEnabled();
        final boolean labelOldTlesEnabled = Configuration.isJulianDateFilterEnabled(); // Re-using your old flag

        int filteredAccessCount = 0;
        Map<String, String> tleStatusMap = new HashMap<>();

        // If labeling is on, pre-read the TLE file to build the status map
        if (labelOldTlesEnabled) {
            tleasyInstance.setStatus("Checking TLE dates...");
            try (BufferedReader reader = new BufferedReader(new FileReader(sanitizedTleFile))) {
                String nameLine, tleLine1, tleLine2;
                while ((nameLine = reader.readLine()) != null) {
                    tleLine1 = reader.readLine();
                    tleLine2 = reader.readLine();

                    if (tleLine1 == null || tleLine2 == null) {
                        break; // End of file
                    }

                    String sanitizedName = nameLine.trim().replace(" ", "_").replace("[", "").replace("]", "");
                    LocalDateTime tleEpoch = TleUtils.parseTleEpoch(tleLine1);

                    if (tleEpoch.isBefore(julianDateLimit)) {
                        tleStatusMap.put(sanitizedName, "Old (>24h)");
                    } else {
                        tleStatusMap.put(sanitizedName, "Current");
                    }
                }
            }
        }

        StringBuilder csvData = new StringBuilder("Satellite,Facility,Start Time (UTCG),Stop Time (UTCG),Start Time (Local),Stop Time (Local),Duration (MM:SS.sss)");
        if (labelOldTlesEnabled) {
            csvData.append(",TLE_Status\n"); // Add new column for TLE status
        } else {
            csvData.append("\n");
        }

        DateTimeFormatter localDateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss");
        DateTimeFormatter stkDateTimeParser = new DateTimeFormatterBuilder()
                .appendPattern("d MMM yyyy HH:mm:ss")
                .optionalStart()
                .appendPattern(".SSS")
                .optionalEnd()
                .toFormatter(Locale.ENGLISH);

        for (String satName : satelliteNames) {
            for (String facilityName : facilityNames) {
                String fromObjectPath = String.format("/Scenario/%s/Facility/%s", scenarioName, facilityName);
                String toObjectPath = String.format("/Scenario/%s/Satellite/%s", scenarioName, satName);

                String accessCommand = String.format("Access %s %s TimePeriod UseScenarioInterval", toObjectPath, fromObjectPath);
                stkConnection.sendConCommand(accessCommand);

                String reportRmCommand = String.format("Report_RM %s Style \"Access\" AccessObject %s TimePeriod UseAccessTimes",
                        toObjectPath, fromObjectPath);
                String reportData = stkConnection.sendConCommand(reportRmCommand);

                if (reportData != null && !reportData.trim().isEmpty() && !reportData.contains("E_CommandFailed")) {
                    String[] reportLines = reportData.split("\\r?\\n");
                    for (int j = 1; j < reportLines.length; j++) {
                        String dataLine = reportLines[j].trim();
                        if (dataLine.isEmpty()) {
                            continue;
                        }

                        String processedLine = dataLine.replaceAll("\\s{2,}", ",");
                        // Split the sanitized line by commas
                        String[] parts = processedLine.split(",");

                        if (parts.length < 4) {
                            continue; // Skip malformed lines.
                        }

                        try {
                            int mins = Configuration.getTimeFilterMinutes();
                            int secs = Configuration.getTimeFilterSeconds();
                            double filterDuration = secs + (mins * 60);
                            double durationInSeconds = Double.parseDouble(parts[3]);
                            if (durationInSeconds <= filterDuration) continue;

                            // Format seconds into minutes and seconds
                            String formattedDuration = formatDuration(durationInSeconds);

                            // Convert UTC to local time
                            String utcStartStr = parts[1];
                            String utcStopStr = parts[2];

                            LocalDateTime utcStart = LocalDateTime.parse(utcStartStr, stkDateTimeParser);
                            LocalDateTime utcStop = LocalDateTime.parse(utcStopStr, stkDateTimeParser);

                            ZonedDateTime localStart = utcStart.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
                            ZonedDateTime localStop = utcStop.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());

                            String localStartStr = localStart.format(localDateTimeFormatter);
                            String localStopStr = localStop.format(localDateTimeFormatter);

                            if (accessFilterEnabled && utcStart.isAfter(accessTimeLimit)) {
                                filteredAccessCount++; // Count it
                                continue; // Skip this row
                            }

                            List<String> csvValues = new ArrayList<>(Arrays.asList(
                                    satName, facilityName, utcStartStr, utcStopStr,
                                    localStartStr, localStopStr, formattedDuration
                            ));

                            if (labelOldTlesEnabled) {
                                String status = tleStatusMap.getOrDefault(satName, "Unknown");
                                csvValues.add(status);
                            }

                            String csvLine = String.join(",", csvValues);
                            csvData.append(csvLine).append("\n");

                        } catch (Exception e) {
                            System.err.println("Could not parse access report line: " + dataLine);
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        // Return the new object instead of just the string
        return new AccessReportResult(csvData.toString(), filteredAccessCount);
    }

    /**
     * Converts a duration from total seconds into a MM:SS.sss formatted string.
     * @param totalSeconds The duration in seconds (e.g., 578.522).
     * @return A formatted string with millisecond precision (e.g., "09:38.522").
     */
    private String formatDuration(double totalSeconds) {
        if (totalSeconds < 0) {
            return "00:00.000";
        }

        // Calculate minutes by dividing the total seconds by 60 and taking the integer part.
        long minutes = (long) (totalSeconds / 60);

        // Calculate the remaining seconds (including the fractional part) using the modulo operator.
        double secondsWithMillis = totalSeconds % 60;

        // Format the minutes with 2 digits and the seconds with 3 decimal places.
        // The format "%06.3f" for seconds ensures padding (e.g., 5.123 -> "05.123").
        return String.format("%02d:%06.3f", minutes, secondsWithMillis);
    }
}