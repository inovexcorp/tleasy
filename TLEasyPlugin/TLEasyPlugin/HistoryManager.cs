using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Windows.Forms;

namespace TLEasyPlugin
{
    public static class HistoryManager
    {
        // Matches Java: System.getProperty("user.home")
        private static readonly string HistoryFilePath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            ".tleasy-history.txt"
        );

        private const int HistoryLimit = 30;

        /// <summary>
        /// Returns the current history list from the file.
        /// </summary>
        public static List<string> GetHistory()
        {
            var list = new List<string>();
            try
            {
                if (File.Exists(HistoryFilePath))
                {
                    list.AddRange(File.ReadAllLines(HistoryFilePath));
                }
            }
            catch { /* Ignore */ }
            return list;
        }

        /// <summary>
        /// Loads the history from the file into the TextBox's AutoComplete source.
        /// </summary>
        public static void LoadHistory(TextBox textBox)
        {
            try
            {
                // Configure the TextBox for Autocomplete
                textBox.AutoCompleteMode = AutoCompleteMode.SuggestAppend;
                textBox.AutoCompleteSource = AutoCompleteSource.CustomSource;

                // Load file if exists
                if (File.Exists(HistoryFilePath))
                {
                    string[] lines = File.ReadAllLines(HistoryFilePath);

                    // Add lines to the TextBox's custom source
                    AutoCompleteStringCollection collection = new AutoCompleteStringCollection();
                    collection.AddRange(lines);
                    textBox.AutoCompleteCustomSource = collection;
                }
            }
            catch (Exception ex)
            {
                // Fail silently (it's just history) but log if needed
                System.Diagnostics.Debug.WriteLine("Error loading history: " + ex.Message);
            }
        }

        /// <summary>
        /// Adds a new entry to the history file.
        /// </summary>
        public static void AddEntry(string entry)
        {
            if (string.IsNullOrWhiteSpace(entry)) return;

            string trimmedEntry = entry.Trim();
            List<string> history = GetHistory();

            // 1. Remove if existing
            if (history.Contains(trimmedEntry))
            {
                history.Remove(trimmedEntry);
            }

            // 2. Insert at top
            history.Insert(0, trimmedEntry);

            // 3. Enforce Limit
            while (history.Count > HistoryLimit)
            {
                history.RemoveAt(history.Count - 1);
            }

            // 4. Save to File
            try
            {
                File.WriteAllLines(HistoryFilePath, history);
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine("Error saving history: " + ex.Message);
            }
        }
    }
}