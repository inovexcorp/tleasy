using AGI.STKObjects;
using AGI.STKUtil;
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using System.Globalization;

namespace TLEasyPlugin.Util
{
    public class AccessReportResult
    {
        public string CsvData { get; set; }
        public int FilteredAccessCount { get; set; }
        public AccessReportResult(string csvData, int filteredAccessCount)
        {
            CsvData = csvData;
            FilteredAccessCount = filteredAccessCount;
        }
    }

    public class AccessReportGenerator
    {
        private readonly IAgStkObjectRoot _stkRoot;
        private readonly TLEasyPlugin _pluginInstance;
        private readonly Action<string> _statusCallback;

        public AccessReportGenerator(IAgStkObjectRoot stkRoot, TLEasyPlugin pluginInstance, Action<string> statusCallback)
        {
            _stkRoot = stkRoot;
            _pluginInstance = pluginInstance;
            _statusCallback = statusCallback;
        }

        public AccessReportResult GenerateAccessReportCsv(string scenarioName, List<TleData> tleDataList, string sanitizedTleFilePath)
        {
            _statusCallback?.Invoke("Calculating access to ground facilities...");

            string facilityListStr = SendCommand("AllInstanceNames / Facility");

            if (string.IsNullOrWhiteSpace(facilityListStr) || facilityListStr.Contains("E_CommandFailed"))
            {
                return null;
            }

            List<string> facilityNames = new List<string>();
            Regex facilityPattern = new Regex(@"/Facility/([^/\s]+)");
            MatchCollection matches = facilityPattern.Matches(facilityListStr);

            foreach (Match match in matches)
            {
                string rawName = match.Groups[1].Value;

                // NEW: Remove "Facility" only if it appears at the very end of the string
                string cleanName = Regex.Replace(rawName, "Facility$", "");

                // Filter by visibility (Object Browser checkbox)
                if (IsFacilityVisible(cleanName, scenarioName))
                {
                    facilityNames.Add(cleanName);
                }
            }

            if (facilityNames.Count == 0)
            {
                Console.WriteLine("No facilities found in the scenario.");
                return null;
            }

            DateTime nowUtc = DateTime.UtcNow;
            DateTime accessTimeLimit = nowUtc.AddHours(24);
            DateTime julianDateLimit = nowUtc.AddDays(-1);

            bool accessFilterEnabled = _pluginInstance.FilterOldAccesses;
            bool labelOldTlesEnabled = _pluginInstance.LabelOldData;
            double filterDuration = (double)_pluginInstance.MinAccessTimeSeconds;

            int filteredAccessCount = 0;
            
            // Build Status Map from TleData directly
            Dictionary<string, string> tleStatusMap = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            if (labelOldTlesEnabled)
            {
                foreach (var tle in tleDataList)
                {
                    try
                    {
                        DateTime epoch = TleUtils.ParseTleEpoch(tle.Line1);
                        if (epoch != DateTime.MinValue && epoch < julianDateLimit)
                            tleStatusMap[tle.SatName] = "Old (>24h)";
                        else
                            tleStatusMap[tle.SatName] = "Current";
                    }
                    catch
                    {
                        tleStatusMap[tle.SatName] = "Unknown"; // Explicitly mark as Unknown on error
                    }
                }
            }

            StringBuilder csvData = new StringBuilder();
            csvData.Append("Satellite,Facility,Start Time (UTCG),Stop Time (UTCG),Start Time (Local),Stop Time (Local),Duration (MM:SS.sss)");
            if (labelOldTlesEnabled) csvData.Append(",TLE_Status\n");
            else csvData.Append("\n");

            int currentSat = 0;
            foreach (var tle in tleDataList)
            {
                string satName = tle.SatName;
                currentSat++;
                _statusCallback?.Invoke($"Calculating Access: {currentSat}/{tleDataList.Count} ({satName})");

                foreach (string facilityName in facilityNames)
                {
                    string fromObjectPath = $"/Scenario/{scenarioName}/Facility/{facilityName}";
                    string toObjectPath = $"/Scenario/{scenarioName}/Satellite/{satName}";

                    string accessCommand = $"Access {toObjectPath} {fromObjectPath} TimePeriod UseScenarioInterval";
                    SendCommand(accessCommand);

                    string reportRmCommand = $"Report_RM {toObjectPath} Style \"Access\" AccessObject {fromObjectPath} TimePeriod UseAccessTimes";
                    string reportData = SendCommand(reportRmCommand);

                    if (!string.IsNullOrWhiteSpace(reportData) && !reportData.Contains("E_CommandFailed"))
                    {
                        string[] reportLines = reportData.Split(new[] { "\n", "\r\n" }, StringSplitOptions.RemoveEmptyEntries);

                        // Skip header line (j=1)
                        for (int j = 1; j < reportLines.Length; j++)
                        {
                            string dataLine = reportLines[j].Trim();

                            if (string.IsNullOrEmpty(dataLine)) continue;

                            string processedLine = Regex.Replace(dataLine, @"\s{2,}", ",");
                            string[] parts = processedLine.Split(',');

                            if (parts.Length < 4) continue;

                            try
                            {
                                if (!double.TryParse(parts[3], out double durationInSeconds)) continue;

                                // 1. Filter by Minimum Duration
                                if (durationInSeconds <= filterDuration) continue;

                                string formattedDuration = FormatDuration(durationInSeconds);

                                string utcStartStr = parts[1];
                                string utcStopStr = parts[2];

                                // Parse as UTC to match STK's UTCG and for accurate comparison with accessTimeLimit
                                DateTime utcStart = DateTime.Parse(utcStartStr, CultureInfo.InvariantCulture, DateTimeStyles.AssumeUniversal | DateTimeStyles.AdjustToUniversal);
                                DateTime utcStop = DateTime.Parse(utcStopStr, CultureInfo.InvariantCulture, DateTimeStyles.AssumeUniversal | DateTimeStyles.AdjustToUniversal);

                                // 2. Filter by Future Access Time (24h+)
                                if (accessFilterEnabled && utcStart > accessTimeLimit)
                                {
                                    filteredAccessCount++;
                                    continue;
                                }

                                string localStartStr = utcStart.ToLocalTime().ToString("d MMM yyyy HH:mm:ss");
                                string localStopStr = utcStop.ToLocalTime().ToString("d MMM yyyy HH:mm:ss");

                                List<string> csvValues = new List<string>
                                {
                                    satName,
                                    facilityName,
                                    utcStartStr,
                                    utcStopStr,
                                    localStartStr,
                                    localStopStr,
                                    formattedDuration
                                };

                                if (labelOldTlesEnabled)
                                {
                                    string status = tleStatusMap.ContainsKey(satName) ? tleStatusMap[satName] : "Unknown";
                                    csvValues.Add(status);
                                }

                                csvData.AppendLine(string.Join(",", csvValues));
                            }
                            catch (Exception ex)
                            {
                                System.Diagnostics.Debug.WriteLine($"Could not parse access report line: {dataLine}. Error: {ex.Message}");
                            }
                        }
                    }
                }
            }
            return new AccessReportResult(csvData.ToString(), filteredAccessCount);
        }

        private bool IsFacilityVisible(string facilityName, string scenarioName)
        {
            try
            {
                string path = $"/Scenario/{scenarioName}/Facility/{facilityName}";
                IAgStkObject obj = _stkRoot.GetObjectFromPath(path);

                if (obj is IAgFacility facility)
                {
                    return facility.Graphics.IsObjectGraphicsVisible;
                }
                return true; // Default to true if cast fails
            }
            catch
            {
                return false; // If object not found, treat as not visible
            }
        }

        private string SendCommand(string command)
        {
            try
            {
                IAgExecCmdResult result = _stkRoot.ExecuteCommand(command);
                if (result.Count > 0)
                {
                    StringBuilder fullResult = new StringBuilder();
                    foreach (var item in result) fullResult.Append(item.ToString() + "\n");
                    return fullResult.ToString();
                }
                return "";
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"SendCommand Failed: {command} | Error: {ex.Message}");
                return "E_CommandFailed: " + ex.Message;
            }
        }

        private string FormatDuration(double totalSeconds)
        {
            if (totalSeconds < 0) return "00:00.000";
            TimeSpan ts = TimeSpan.FromSeconds(totalSeconds);
            return string.Format("{0:D2}:{1:D2}.{2:D3}", (int)ts.TotalMinutes, ts.Seconds, ts.Milliseconds);
        }
    }
}