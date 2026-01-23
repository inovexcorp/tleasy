using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace TLEasyPlugin.Util // Ensure this matches the 'using' in your Control
{
    // 1. We need this simple container to pass data back to the Control
    public class TleData
    {
        public string SatName { get; set; }
        public string NoradId { get; set; }
        public string Line1 { get; set; }
        public string Line2 { get; set; }
    }

    public static class TleFileProcessor
    {
        // --- NEW METHOD: Returns List<TleData> for the Plugin to use ---
        public static List<TleData> GetTlesByIds(string inputFilePath, List<int> targetIds)
        {
            var results = new List<TleData>();
            var targetSet = new HashSet<string>(targetIds.Select(id => id.ToString()));

            if (!File.Exists(inputFilePath)) return results;

            // Read all lines into a list to process them in chunks (Matching Java logic)
            List<string> lines = File.ReadAllLines(inputFilePath).Select(l => l.Trim()).ToList();
            
            int i = 0;
            while (i < lines.Count)
            {
                string currentLine = lines[i];

                // Skip any blank lines
                if (string.IsNullOrWhiteSpace(currentLine))
                {
                    i++;
                    continue;
                }

                string nameLine;
                string tleLine1;
                string tleLine2;

                // Case 1: The current line is a TLE line 1 (2-line format or missing name)
                if (currentLine.StartsWith("1 "))
                {
                    tleLine1 = currentLine;

                    // The next line must be TLE line 2
                    if (i + 1 < lines.Count && lines[i + 1].StartsWith("2 "))
                    {
                        tleLine2 = lines[i + 1];
                        // Extract ID from line 1 to generate a default name
                        string noradId = tleLine1.Substring(2, 5).Trim();
                        nameLine = "tle-" + noradId;
                        i += 2; // Consumed two lines
                    }
                    else
                    {
                        // Malformed: Skip it
                        i++;
                        continue;
                    }
                }
                // Case 2: The current line is a name (standard 3-line format)
                else
                {
                    nameLine = currentLine;

                    // The next two lines must be TLE line 1 and line 2
                    if (i + 2 < lines.Count &&
                        lines[i + 1].StartsWith("1 ") &&
                        lines[i + 2].StartsWith("2 "))
                    {
                        tleLine1 = lines[i + 1];
                        tleLine2 = lines[i + 2];
                        i += 3; // Consumed three lines
                    }
                    else
                    {
                        // Malformed: Skip it
                        i++;
                        continue;
                    }
                }

                // Check Match
                string currentNoradId = ExtractIdRobust(tleLine2);

                if (targetSet.Contains(currentNoradId))
                {
                    // Use extracted ID from Line 1 if name was auto-generated, to ensure consistency
                    if (nameLine.StartsWith("tle-"))
                    {
                        string idFromLine1 = ExtractIdRobust(tleLine1);
                        nameLine = "tle-" + idFromLine1;
                    }

                    string satName = TleUtils.SanitizeStkName(nameLine);

                    results.Add(new TleData
                    {
                        SatName = satName,
                        NoradId = currentNoradId,
                        Line1 = tleLine1,
                        Line2 = tleLine2
                    });
                }
            }

            return results;
        }

        // --- EXISTING METHOD: Writes to a File (Kept for your 'Load STK' legacy button) ---
        public static long FilterTleFile(string inputFilePath, string outputFilePath, List<string> targetIds)
        {
            HashSet<string> targetSet = new HashSet<string>(targetIds);
            long counter = 0;

            if (!File.Exists(inputFilePath))
            {
                throw new FileNotFoundException("Source TLE file not found", inputFilePath);
            }

            List<string> lines = File.ReadAllLines(inputFilePath).Select(l => l.Trim()).ToList();
            
            using (StreamWriter writer = new StreamWriter(outputFilePath))
            {
                int i = 0;
                while (i < lines.Count)
                {
                    string currentLine = lines[i];
                    if (string.IsNullOrWhiteSpace(currentLine))
                    {
                        i++;
                        continue;
                    }

                    string nameLine;
                    string tleLine1;
                    string tleLine2;

                    if (currentLine.StartsWith("1 "))
                    {
                        tleLine1 = currentLine;
                        if (i + 1 < lines.Count && lines[i + 1].StartsWith("2 "))
                        {
                            tleLine2 = lines[i + 1];
                            string noradId = ExtractIdRobust(tleLine1);
                            nameLine = "tle-" + noradId;
                            i += 2;
                        }
                        else { i++; continue; }
                    }
                    else
                    {
                        nameLine = currentLine;
                        if (i + 2 < lines.Count && lines[i + 1].StartsWith("1 ") && lines[i + 2].StartsWith("2 "))
                        {
                            tleLine1 = lines[i + 1];
                            tleLine2 = lines[i + 2];
                            i += 3;
                        }
                        else { i++; continue; }
                    }

                    string currentNoradId = ExtractIdRobust(tleLine2);

                    if (targetSet.Contains(currentNoradId))
                    {
                        string sanitizedName = TleUtils.SanitizeStkName(nameLine);
                        writer.WriteLine(sanitizedName);
                        writer.WriteLine(tleLine1);
                        writer.WriteLine(tleLine2);
                        counter++;
                    }
                }
            }
            return counter;
        }

        private static string ExtractIdRobust(string line)
        {
            if (string.IsNullOrWhiteSpace(line)) return "";
            string[] parts = line.Split(new[] { ' ', '\t' }, StringSplitOptions.RemoveEmptyEntries);
            // Index 1 is the ID (Index 0 is the line number "1" or "2")
            if (parts.Length > 1)
            {
                return parts[1].Trim().TrimStart('0');
            }
            return "";
        }
    }
}