using AGI.STKObjects;
using AGI.STKUtil;
using AGI.Ui.Plugins;
using stdole;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Windows.Forms;
using TLEasyPlugin.Util;

namespace TLEasyPlugin
{
    public partial class TLEasyPluginControl : UserControl, IAgUiPluginEmbeddedControl
    {
        private IAgUiPluginEmbeddedControlSite m_pEmbeddedControlSite;
        private TLEasyPlugin m_uiplugin;
        private AgStkObjectRoot m_root;
        private List<string> _history;

        public TLEasyPluginControl()
        {
            InitializeComponent();
            SetupCustomAutocomplete();
            LoadImages();
        }

        private void SetupCustomAutocomplete()
        {
            _history = HistoryManager.GetHistory();
            
            // Text events
            txtIDs.TextChanged += TxtIDs_TextChanged;
            txtIDs.KeyDown += TxtIDs_KeyDown;
            txtIDs.LostFocus += TxtIDs_LostFocus;
            txtIDs.Click += (s, e) => ShowSuggestions(); // Show on click too

            // List events
            lstSuggestions.Click += LstSuggestions_Click;
            lstSuggestions.KeyDown += LstSuggestions_KeyDown;
        }

        private void LstSuggestions_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.KeyCode == Keys.Enter)
            {
                SelectSuggestion();
                e.Handled = true;
            }
            else if (e.KeyCode == Keys.Escape)
            {
                lstSuggestions.Visible = false;
                txtIDs.Focus();
                e.Handled = true;
            }
        }

        protected override bool ProcessCmdKey(ref Message msg, Keys keyData)
        {
            if (lstSuggestions.Visible)
            {
                if (keyData == Keys.Enter || keyData == Keys.Tab)
                {
                    SelectSuggestion();
                    return true; // Mark handled
                }
                else if (keyData == Keys.Escape)
                {
                    lstSuggestions.Visible = false;
                    txtIDs.Focus();
                    return true;
                }
                else if (keyData == Keys.Down)
                {
                    if (lstSuggestions.Items.Count > 0)
                    {
                        if (lstSuggestions.SelectedIndex < lstSuggestions.Items.Count - 1)
                            lstSuggestions.SelectedIndex++;
                        else
                            lstSuggestions.SelectedIndex = 0; // Wrap around
                    }
                    return true;
                }
                else if (keyData == Keys.Up)
                {
                    if (lstSuggestions.Items.Count > 0)
                    {
                        if (lstSuggestions.SelectedIndex > 0)
                            lstSuggestions.SelectedIndex--;
                        else
                            lstSuggestions.SelectedIndex = lstSuggestions.Items.Count - 1; // Wrap around
                    }
                    return true;
                }
            }
            return base.ProcessCmdKey(ref msg, keyData);
        }

        private void TxtIDs_TextChanged(object sender, EventArgs e)
        {
            ShowSuggestions();
        }

        private void ShowSuggestions()
        {
            string input = txtIDs.Text.Trim();
            // Get last part if comma separated? The Java app did this.
            // "11111, 22..." -> suggest for "22..."
            // For simplicity, let's just match start of string for now, or match Java logic?
            // Java logic:
            // int lastSeparator = Math.max(lastComma, lastDash);
            // String currentPart = ... substring(lastSeparator + 1);
            
            // Simpler C# implementation:
            string currentPart = input;
            int lastComma = input.LastIndexOf(',');
            if (lastComma >= 0) currentPart = input.Substring(lastComma + 1).Trim();

            if (string.IsNullOrEmpty(currentPart))
            {
                lstSuggestions.Visible = false;
                return;
            }

            var matches = _history.Where(h => h.StartsWith(currentPart, StringComparison.OrdinalIgnoreCase)).ToList();
            
            if (matches.Any())
            {
                lstSuggestions.DataSource = matches;
                lstSuggestions.Visible = true;
                lstSuggestions.BringToFront();
            }
            else
            {
                lstSuggestions.Visible = false;
            }
        }

        private void TxtIDs_KeyDown(object sender, KeyEventArgs e)
        {
            if (!lstSuggestions.Visible) return;

            if (e.KeyCode == Keys.Down)
            {
                if (lstSuggestions.SelectedIndex < lstSuggestions.Items.Count - 1)
                    lstSuggestions.SelectedIndex++;
                e.Handled = true;
            }
            else if (e.KeyCode == Keys.Up)
            {
                if (lstSuggestions.SelectedIndex > 0)
                    lstSuggestions.SelectedIndex--;
                e.Handled = true;
            }
            else if (e.KeyCode == Keys.Enter || e.KeyCode == Keys.Tab)
            {
                SelectSuggestion();
                e.Handled = true;
                e.SuppressKeyPress = true; // Prevent beep
            }
            else if (e.KeyCode == Keys.Escape)
            {
                lstSuggestions.Visible = false;
                e.Handled = true;
            }
        }

        private void TxtIDs_LostFocus(object sender, EventArgs e)
        {
            // Delay hide to allow ListBox click to register
            Timer t = new Timer();
            t.Interval = 200;
            t.Tick += (s, args) =>
            {
                if (!lstSuggestions.Focused) // Check if focus moved to list
                    lstSuggestions.Visible = false;
                t.Stop();
            };
            t.Start();
        }

        private void LstSuggestions_Click(object sender, EventArgs e)
        {
            SelectSuggestion();
        }

        private void SelectSuggestion()
        {
            if (lstSuggestions.SelectedItem == null) return;
            string selection = lstSuggestions.SelectedItem.ToString();

            // Replace current part
            string text = txtIDs.Text;
            int lastComma = text.LastIndexOf(',');
            
            if (lastComma >= 0)
            {
                txtIDs.Text = text.Substring(0, lastComma + 1) + " " + selection;
            }
            else
            {
                txtIDs.Text = selection;
            }
            
            txtIDs.SelectionStart = txtIDs.Text.Length;
            lstSuggestions.Visible = false;
            txtIDs.Focus();
        }

        public void SetSite(IAgUiPluginEmbeddedControlSite Site)
        {
            m_pEmbeddedControlSite = Site;
            m_uiplugin = m_pEmbeddedControlSite.Plugin as TLEasyPlugin;
            m_root = m_uiplugin.STKRoot;
        }

        // --- Event Handlers ---
        private void txtIDs_TextChanged(object sender, EventArgs e)
        {
            if (m_uiplugin != null)
            {
                m_uiplugin.SatelliteIDList = txtIDs.Text;
            }
        }

        private void btnLoadSTK_Click(object sender, EventArgs e)
        {
            // 1. UI Feedback
            lblStatus.Text = "Processing...";
            lblStatus.ForeColor = Color.Orange;
            Application.DoEvents();

            try
            {
                // 2. Run the new logic
                RunAnalysisWorkflow();

                // 3. Success Feedback
                lblStatus.Text = "Done";
                lblStatus.ForeColor = Color.Green;
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Error: {ex.Message}");
                lblStatus.Text = "Error";
                lblStatus.ForeColor = Color.Red;
            }
        }

        private void RunAnalysisWorkflow()
        {
            // 0. ENSURE SCENARIO
            m_uiplugin.EnsureScenarioExists();

            // 1. GET CONFIG
            string rawIds = txtIDs.Text;
            HistoryManager.AddEntry(rawIds); // Update history file
            _history = HistoryManager.GetHistory(); // Reload local cache
            string sourceTlePath = m_uiplugin.tleFilePath;

            // Check if path is valid
            if (string.IsNullOrEmpty(sourceTlePath))
            {
                MessageBox.Show("Please set a TLE File Path in the Plugin Configuration page first.");
                return;
            }

            // Parse IDs using the robust utility method
            List<int> targetIdsInt = ColorUtils.ParseAndExpandIds(rawIds);
            if (targetIdsInt.Count == 0)
            {
                MessageBox.Show("No valid satellite IDs were entered.");
                return;
            }
            List<string> targetIds = targetIdsInt.Select(id => id.ToString()).ToList();

            // 2. BUILD COLOR MAP
            var idToColorMap = ColorUtils.BuildColorMap(rawIds);
            var nameToColorMap = new Dictionary<string, string>();
 
            // 3. PRE-PROCESS TLE (To get the names of the sats we are about to load)
            var tleDataList = TleFileProcessor.GetTlesByIds(sourceTlePath, targetIdsInt);

            if (tleDataList.Count == 0)
            {
                MessageBox.Show("No matching satellites found in the source file.");
                return;
            }

            // --- WARNING CHECKS ---
            // 1. Check if the file itself is stale (> 24 hours)
            if (System.IO.File.Exists(sourceTlePath))
            {
                DateTime lastWrite = System.IO.File.GetLastWriteTime(sourceTlePath);
                if ((DateTime.Now - lastWrite).TotalHours > 24)
                {
                    MessageBox.Show($"Warning: The source TLE file '{System.IO.Path.GetFileName(sourceTlePath)}'\n" +
                                    "has not been modified in over 24 hours.",
                                    "Stale TLE File Warning",
                                    MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }

            // 2. Check for old TLE entries if labeling is enabled
            if (m_uiplugin.LabelOldData)
            {
                int oldTleCount = 0;
                // Use UTC comparison consistent with other parts of the app
                DateTime limit = DateTime.UtcNow.AddDays(-1);

                foreach (var tle in tleDataList)
                {
                    DateTime epoch = TleUtils.ParseTleEpoch(tle.Line1);
                    if (epoch < limit) oldTleCount++;
                }

                if (oldTleCount > 0)
                {
                    MessageBox.Show($"Warning: {oldTleCount} TLE entries are older than 24 hours.",
                                    "Old TLE Warning",
                                    MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            // ----------------------

            // LINK NAMES TO COLORS
            foreach (var tleData in tleDataList)
            {
                if (idToColorMap.ContainsKey(tleData.NoradId))
                {
                    nameToColorMap[tleData.SatName] = idToColorMap[tleData.NoradId];
                }
            }

            // 4. LOAD INTO STK (Using your Loader)
            var loader = new SatelliteLoader((IAgStkObjectRoot)m_root, (msg) =>
            {
                lblStatus.Text = msg;
                Application.DoEvents();
            });

            loader.LoadSatellites(tleDataList, sourceTlePath, nameToColorMap);

            // 5. GENERATE REPORT
            // We delegate ALL calculation and formatting to the Generator now.
            var generator = new AccessReportGenerator((IAgStkObjectRoot)m_root, m_uiplugin, (msg) =>
            {
                lblStatus.Text = msg;
                Application.DoEvents();
            });
            // Get Scenario Name safely
            string scenarioName = "TLE_Analysis";
            if (m_root.CurrentScenario != null)
            {
                // Cast to object to access InstanceName
                scenarioName = ((IAgStkObject)m_root.CurrentScenario).InstanceName;
            }

            var reportResult = generator.GenerateAccessReportCsv(scenarioName, tleDataList, sourceTlePath);

            // Notify about filtered accesses
            if (reportResult != null && reportResult.FilteredAccessCount > 0)
            {
                MessageBox.Show($"{reportResult.FilteredAccessCount} future accesses were filtered from the report.",
                                "Access Filter Applied",
                                MessageBoxButtons.OK, MessageBoxIcon.Information);
            }

            if (reportResult != null && !string.IsNullOrEmpty(reportResult.CsvData))
            {
                // 5. SAVE CSV
                // Pass the pre-formatted string from the generator
                SaveResultsToCsv(reportResult.CsvData);
            }
            else
            {
                MessageBox.Show("Analysis complete, but no access intervals matched your criteria.");
            }
        }

        private void LoadImages()
        {
            try
            {
                var assembly = System.Reflection.Assembly.GetExecutingAssembly();
                using (var stream = assembly.GetManifestResourceStream("TLEasyPlugin.tleasy.png"))
                {
                    if (stream != null)
                    {
                        Image original = Image.FromStream(stream);
                        // Add 25% padding to shrink the visual size of the logo
                        pictureBoxLeft.Image = AddPadding(original, original.Width / 4);
                    }
                }
                using (var stream = assembly.GetManifestResourceStream("TLEasyPlugin.r1foundry.png"))
                {
                    if (stream != null) pictureBoxRight.Image = Image.FromStream(stream);
                }
            }
            catch { /* Ignore image loading errors */ }
        }

        private Image AddPadding(Image original, int padding)
        {
            int newWidth = original.Width + (padding * 2);
            int newHeight = original.Height + (padding * 2);
            Bitmap padded = new Bitmap(newWidth, newHeight);
            using (Graphics g = Graphics.FromImage(padded))
            {
                g.Clear(Color.Transparent);
                g.DrawImage(original, padding, padding, original.Width, original.Height);
            }
            return padded;
        }

        private void SaveResultsToCsv(string csvContent)
        {
            using (var sfd = new SaveFileDialog())
            {
                sfd.Filter = "CSV Files (*.csv)|*.csv";
                sfd.FileName = "AccessReport.csv";

                if (sfd.ShowDialog() == DialogResult.OK)
                {
                    System.IO.File.WriteAllText(sfd.FileName, csvContent);
                    MessageBox.Show("Report generated successfully!");
                }
            }
        }

        // Interface Requirements
        public void OnClosing() { }
        public void OnSaveModified() { }
        public IPictureDisp GetIcon() { return null; }

        private void TLEasyPluginControl_Load(object sender, EventArgs e)
        {

        }

    }
}