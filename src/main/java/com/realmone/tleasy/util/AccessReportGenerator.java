package com.realmone.tleasy.util;

import com.realmone.tleasy.Configuration;
import com.realmone.tleasy.TLEasy;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Generates a string containing access report data formatted as a fixed-width text file,
     * similar to a standard STK report.
     *
     * @param scenarioName The name of the STK scenario.
     * @param satelliteNames A list of satellite names to include in the report.
     * @return A string formatted as a text report.
     * @throws IOException If the STK connection fails.
     */
    public String generateAccessReportTxt(String scenarioName, List<String> satelliteNames) throws IOException {
        tleasyInstance.setStatus("Calculating access to ground facilities...");
        String facilityListStr = stkConnection.sendConCommand("AllInstanceNames / Facility");
        if (facilityListStr.trim().isEmpty() || facilityListStr.contains("E_CommandFailed")) {
            return ""; // No facilities to check against or command failed
        }

        List<String> facilityNames = new ArrayList<>();
        Pattern pattern = Pattern.compile("/Facility/([^/]+)");
        Matcher matcher = pattern.matcher(facilityListStr);
        while (matcher.find()) {
            facilityNames.add(matcher.group(1).replaceAll("Facility$", ""));
        }

        if (facilityNames.isEmpty()) {
            System.out.println("No facilities found in the scenario.");
            return "";
        }

        StringBuilder reportContent = new StringBuilder();

        DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        reportContent.append(LocalDateTime.now().format(timestampFormatter)).append("\n\n");
        reportContent.append("Coverage Intervals\n\n");

        // Define the fixed-width format for data columns
        String format = "%-25s %8s %-25s %-25s %-25s %-25s %22s\n";

        // Date/Time formatters
        DateTimeFormatter stkDateTimeParser = new DateTimeFormatterBuilder()
                .appendPattern("d MMM yyyy HH:mm:ss")
                .optionalStart().appendPattern(".SSS").optionalEnd()
                .toFormatter(Locale.ENGLISH);
        DateTimeFormatter localDateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss");

        // Loop through each facility to create a separate report section
        for (String facilityName : facilityNames) {
            StringBuilder facilityData = new StringBuilder(); // Buffer data to see if any access exists

            for (String satName : satelliteNames) {
                String fromObjectPath = String.format("/Scenario/%s/Facility/%s", scenarioName, facilityName);
                String toObjectPath = String.format("/Scenario/%s/Satellite/%s", scenarioName, satName);
                String accessCommand = String.format("Access %s %s TimePeriod UseScenarioInterval", toObjectPath, fromObjectPath);
                stkConnection.sendConCommand(accessCommand);
                String reportRmCommand = String.format("Report_RM %s Style \"Access\" AccessObject %s TimePeriod UseAccessTimes", toObjectPath, fromObjectPath);
                String reportData = stkConnection.sendConCommand(reportRmCommand);

                if (reportData != null && !reportData.trim().isEmpty() && !reportData.contains("E_CommandFailed")) {
                    String[] reportLines = reportData.split("\\r?\\n");
                    for (int j = 1; j < reportLines.length; j++) {
                        String dataLine = reportLines[j].trim();
                        if (dataLine.isEmpty()) continue;

                        String processedLine = dataLine.replaceAll("\\s{2,}", ",");
                        String[] parts = processedLine.split(",");
                        if (parts.length < 4) continue;

                        try {
                            int mins = Configuration.getTimeFilterMinutes();
                            int secs = Configuration.getTimeFilterSeconds();
                            double filterDuration = secs + (mins * 60);
                            double durationInSeconds = Double.parseDouble(parts[3]);
                            if (durationInSeconds <= filterDuration) continue;

                            String formattedDuration = formatDuration(durationInSeconds);
                            String utcStartStr = parts[1];
                            String utcStopStr = parts[2];
                            LocalDateTime utcStart = LocalDateTime.parse(utcStartStr, stkDateTimeParser);
                            LocalDateTime utcStop = LocalDateTime.parse(utcStopStr, stkDateTimeParser);
                            ZonedDateTime localStart = utcStart.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
                            ZonedDateTime localStop = utcStop.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
                            String localStartStr = localStart.format(localDateTimeFormatter);
                            String localStopStr = localStop.format(localDateTimeFormatter);

                            facilityData.append(String.format(format,
                                    satName, parts[0], utcStartStr, utcStopStr,
                                    localStartStr, localStopStr, formattedDuration));

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            // If any valid access is found for this facility, add its section to the main report
            if (facilityData.length() > 0) {
                reportContent.append(String.format("Coverage for %s\n", facilityName));
                reportContent.append("-----------------------------------------------------------------------------------------------------------------------------------------------------\n");

                // Add column headers, using the same formatter for alignment
                reportContent.append(String.format(format,
                        "Satellite", "Access #", "Start Time (UTCG)", "Stop Time (UTCG)",
                        "Start Time (Local)", "Stop Time (Local)", "Duration (MM:SS.sss)"));

                // Add separator lines under the headers, also using the formatter
                reportContent.append(String.format(format,
                        "-------------------------", "--------", "-------------------------", "-------------------------",
                        "-------------------------", "-------------------------", "----------------------"));

                // Append the buffered data rows
                reportContent.append(facilityData);
                reportContent.append("\n"); // Add space before the next facility's section
            }
        }

        return reportContent.toString();
    }


    /**
     * Computes access between all loaded satellites and existing facilities, then generates a CSV report.
     * @param scenarioName The name of the target scenario.
     * @param satelliteNames A list of satellite names to calculate access for.
     * @return A string containing the access report in CSV format.
     */
    public String generateAccessReportCsv(String scenarioName, List<String> satelliteNames) throws IOException {
        tleasyInstance.setStatus("Calculating access to ground facilities...");
        String facilityListStr = stkConnection.sendConCommand("AllInstanceNames / Facility");
        if (facilityListStr.trim().isEmpty() || facilityListStr.contains("E_CommandFailed")) {
            return null;
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

        StringBuilder csvData = new StringBuilder("Satellite,Facility,Access Number,Start Time (UTCG),Stop Time (UTCG),Start Time (Local),Stop Time (Local),Duration (MM:SS.sss)\n");

        DateTimeFormatter stkDateTimeParser = new DateTimeFormatterBuilder()
                .appendPattern("d MMM yyyy HH:mm:ss")
                .optionalStart()
                .appendPattern(".SSS")
                .optionalEnd()
                .toFormatter(Locale.ENGLISH);

        DateTimeFormatter localDateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss");

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

                            String csvLine = String.join(",",
                                    satName, facilityName, parts[0], utcStartStr, utcStopStr,
                                    localStartStr, localStopStr, formattedDuration);
                            csvData.append(csvLine).append("\n");

                        } catch (Exception e) {
                            System.err.println("Could not parse access report line: " + dataLine);
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return csvData.toString();
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