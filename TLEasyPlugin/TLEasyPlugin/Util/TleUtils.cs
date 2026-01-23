using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;

namespace TLEasyPlugin.Util
{
    public static class TleUtils
    {
        /// <summary>
        /// Parses the text from the ID input field to extract valid ID numbers.
        /// Supports single 5-digit IDs ("12345") and ranges ("12345-12350").
        /// </summary>
        public static HashSet<string> ParseIdentifiers(string inputString)
        {
            if (string.IsNullOrWhiteSpace(inputString))
                return new HashSet<string>();

            string input = inputString.Trim();
            HashSet<string> result = new HashSet<string>();

            // Split by comma, ignoring surrounding whitespace
            // Java: input.split("\\s*,\\s*")
            string[] parts = Regex.Split(input, @"\s*,\s*");

            foreach (string part in parts)
            {
                // 1. Check for strict 5-digit identifier
                if (Regex.IsMatch(part, @"^\d{5}$"))
                {
                    result.Add(part);
                }
                // 2. Check for Range (e.g., 12345 - 12350)
                else if (Regex.IsMatch(part, @"^\d{5}\s*-\s*\d{5}$"))
                {
                    string[] rangeParts = Regex.Split(part, @"\s*-\s*");

                    if (int.TryParse(rangeParts[0], out int start) &&
                        int.TryParse(rangeParts[1], out int end))
                    {
                        if (start > end)
                        {
                            throw new ArgumentException($"Range must be in ascending order: {part}");
                        }

                        // Add the range
                        foreach (int id in GetNumbersBetween(start, end))
                        {
                            result.Add(id.ToString("D5"));
                        }
                    }
                }
                // Optional: You could throw an error here if a part doesn't match either pattern
                // else { throw new ArgumentException("Invalid format: " + part); }
            }

            return result;
        }

        public static IEnumerable<int> GetNumbersBetween(int start, int end)
        {
            if (start > end)
            {
                throw new ArgumentException("Start number must be less than or equal to end number.");
            }

            // Enumerable.Range is the C# clean way to generate a sequence
            return Enumerable.Range(start, end - start + 1);
        }

        /// <summary>
        /// Parses the epoch from TLE Line 1 into a C# DateTime object.
        /// TLE epoch format is YYDDD.FFFFFFFF.
        /// </summary>
        public static DateTime ParseTleEpoch(string tleLine1)
        {
            try
            {
                // Try Strict Column Parsing First (Standard TLE)
                if (tleLine1.Length >= 32)
                {
                    string epochStr = tleLine1.Substring(18, 14); 
                    return ParseEpochString(epochStr);
                }
            }
            catch { /* Fallback to robust parsing */ }

            try
            {
                // Fallback: Token-based parsing (Robust for user edits)
                // Line 1 tokens: "1", "ID", "Class", "Epoch"
                string[] parts = tleLine1.Split(new[] { ' ', '\t' }, StringSplitOptions.RemoveEmptyEntries);
                if (parts.Length >= 4)
                {
                    return ParseEpochString(parts[3]);
                }
            }
            catch { /* Parsing failed */ }

            return DateTime.MinValue;
        }

        private static DateTime ParseEpochString(string epochStr)
        {
            if (epochStr.Length < 4) throw new ArgumentException("Invalid epoch length");

            // Year Logic
            int year = int.Parse(epochStr.Substring(0, 2));
            year += (year < 57) ? 2000 : 1900;

            // Day Logic
            double dayOfYearWithFraction = double.Parse(epochStr.Substring(2));
            int dayOfYear = (int)dayOfYearWithFraction;
            double fractionOfDay = dayOfYearWithFraction - dayOfYear;

            DateTime date = new DateTime(year, 1, 1);
            date = date.AddDays(dayOfYear - 1);
            date = date.AddSeconds(fractionOfDay * 86400.0);

            return date;
        }

        public static bool IsTleOlderThan(string line1, double hours)
        {
            // Parse the Epoch Year and Day from TLE Line 1 (cols 19-32)
            // Convert to DateTime
            // Compare to DateTime.UtcNow
            // Return true if difference.TotalHours > hours
            return false; // (Implement parsing logic here)
        }

        public static bool IsDateMoreThan24HoursFuture(string stkDateString)
        {
            // Convert STK date string (e.g., "1 Jan 2024 12:00:00.00") to C# DateTime
            // Compare with DateTime.Now.AddHours(24)
            return false; // (Implement parsing logic here)
        }

        /// <summary>
        /// Sanitizes a raw name to make it a valid STK object name.
        /// This replaces spaces, removes invalid characters, and prefixes with "Sat_" if it starts with a number.
        /// </summary>
        public static string SanitizeStkName(string rawName)
        {
            if (string.IsNullOrEmpty(rawName)) return "";

            string clean = rawName.Trim().Replace(" ", "_");
            clean = Regex.Replace(clean, @"[\[\]\(\)]", "");    // Remove brackets
            clean = Regex.Replace(clean, @"[^a-zA-Z0-9_]", ""); // DISALLOW dashes and slashes
            clean = clean.Trim('_');

            if (string.IsNullOrEmpty(clean)) return "Invalid_Name";

            if (char.IsDigit(clean[0]))
            {
                clean = "Sat_" + clean;
            }

            return clean;
        }
    }
}