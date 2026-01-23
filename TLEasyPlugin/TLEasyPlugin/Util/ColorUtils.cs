using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text.RegularExpressions;

namespace TLEasyPlugin.Util
{
    public static class ColorUtils
    {
        private const float GOLDEN_RATIO_CONJUGATE = 0.618033988749f;

        /// <summary>
        /// Builds a map of NORAD ID to a distinct hex color string based on contiguous ID groups.
        /// </summary>
        public static Dictionary<string, string> BuildColorMap(string rawInput)
        {
            var idToColor = new Dictionary<string, string>();
            if (string.IsNullOrWhiteSpace(rawInput)) return idToColor;

            // 1. Parse all IDs from ranges and individual entries into a sorted list
            List<int> sortedIds = ParseAndExpandIds(rawInput);
            if (sortedIds.Count == 0) return idToColor;

            // 2. Iterate the sorted list and assign perceptually distinct colors to groups
            float currentHue = new Random().NextFloat(); // Start with a random hue
            string currentGroupColor = ConvertHueToHex(currentHue);
            int lastId = -1;

            foreach (int id in sortedIds)
            {
                if (lastId != -1 && id != lastId + 1)
                {
                    // If the current ID is not sequential to the last one, generate a new distinct color
                    currentHue = (currentHue + GOLDEN_RATIO_CONJUGATE) % 1.0f;
                    currentGroupColor = ConvertHueToHex(currentHue);
                }

                idToColor[id.ToString()] = currentGroupColor;
                lastId = id;
            }

            return idToColor;
        }

        /// <summary>
        /// Converts a hue value (0-1) into an STK-compatible hex color string.
        /// </summary>
        private static string ConvertHueToHex(float hue)
        {
            // Use high saturation (0.9) and high brightness (0.9) for vivid, distinct colors
            Color newColor = ColorFromAhsb(1.0f, hue, 0.9f, 0.9f);
            // Convert the java.awt.Color object to an STK-compatible hex string
            return $"#{newColor.R:X2}{newColor.G:X2}{newColor.B:X2}";
        }
        
        /// <summary>
        /// Parses a raw string of IDs and ranges (e.g., "1-3, 5, 8") into a sorted list of integers.
        /// </summary>
        public static List<int> ParseAndExpandIds(string input)
        {
            var idSet = new HashSet<int>();
            string[] parts = Regex.Split(input, @"[\s,]+");

            foreach (string part in parts)
            {
                if (string.IsNullOrWhiteSpace(part)) continue;

                if (part.Contains('-'))
                {
                    string[] rangeParts = part.Split('-');
                    if (rangeParts.Length == 2 && int.TryParse(rangeParts[0], out int start) && int.TryParse(rangeParts[1], out int end))
                    {
                        for (int i = start; i <= end; i++)
                        {
                            idSet.Add(i);
                        }
                    }
                }
                else
                {
                    if (int.TryParse(part, out int id))
                    {
                        idSet.Add(id);
                    }
                }
            }
            return idSet.OrderBy(id => id).ToList();
        }

        /// <summary>
        /// Creates a Color object from alpha, hue, saturation and brightness values.
        /// </summary>
        private static Color ColorFromAhsb(float alpha, float hue, float saturation, float brightness)
        {
            if (saturation == 0)
            {
                int val = (int)(brightness * 255);
                return Color.FromArgb((int)(alpha * 255), val, val, val);
            }

            float hueAngle = hue * 360f;
            int hi = Convert.ToInt32(Math.Floor(hueAngle / 60)) % 6;
            float f = hueAngle / 60f - (float)Math.Floor(hueAngle / 60);

            brightness = brightness * 255;
            int v = Convert.ToInt32(brightness);
            int p = Convert.ToInt32(brightness * (1 - saturation));
            int q = Convert.ToInt32(brightness * (1 - f * saturation));
            int t = Convert.ToInt32(brightness * (1 - (1 - f) * saturation));
            
            int r, g, b;
            switch (hi)
            {
                case 0: r = v; g = t; b = p; break;
                case 1: r = q; g = v; b = p; break;
                case 2: r = p; g = v; b = t; break;
                case 3: r = p; g = q; b = v; break;
                case 4: r = t; g = p; b = v; break;
                default: r = v; g = p; b = q; break;
            }

            return Color.FromArgb((int)(alpha * 255), r, g, b);
        }
    }

    public static class RandomExtension
    {
        public static float NextFloat(this Random rand)
        {
            return (float)rand.NextDouble();
        }
    }
}
