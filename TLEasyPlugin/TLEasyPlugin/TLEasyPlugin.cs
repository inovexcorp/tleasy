using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using AGI.Ui.Application;
using AGI.Ui.Core;
using AGI.Ui.Plugins;
using stdole;
using System.Runtime.InteropServices;
using System.Windows.Forms;
using AGI.STKObjects;
using AGI.STKUtil;
using Microsoft.Win32;

namespace TLEasyPlugin
{
    [Guid("E4F3AEA0-4326-42C2-AA24-1DF18D49111A")]
    [ProgId("RealmOne.TLEasyPlugin")]
    [ClassInterface(ClassInterfaceType.None)]
    public class TLEasyPlugin : IAgUiPlugin, IAgUiPluginCommandTarget
    {
        private IAgUiPluginSite m_psite;
        private AgStkObjectRoot m_root;

        public bool LabelOldData { get; set; } = false;
        public bool FilterOldAccesses { get; set; } = false;
        public int MinAccessTimeSeconds { get; set; } = 420;
        public string TLEFileLocation { get; set; } = "";
        public string SatelliteIDList { get; set; } = "";
        public string ScenarioSaveFilePath { get; set; } = "";

        public AgStkObjectRoot STKRoot
        {
            get { return m_root; }
        }

        public string m_tleFilePath = "C:\\Users\\admin\\Desktop\\tlez\\data.tle";

        public string tleFilePath
        {
            get { return m_tleFilePath; }
            set { m_tleFilePath = value; }
        }

        public void OnStartup(IAgUiPluginSite PluginSite)
        {
            m_psite = PluginSite;
            IAgUiApplication AgUiApp = m_psite.Application;
            m_root = AgUiApp.Personality2 as AgStkObjectRoot;
            LoadSettings();
        }

        public void OnShutdown()
        {
            SaveSettings();
            m_psite = null;
        }

        public void OnDisplayConfigurationPage(IAgUiPluginConfigurationPageBuilder ConfigPageBuilder)
        {
            ConfigPageBuilder.AddCustomUserControlPage(this, this.GetType().Assembly.Location, typeof(TLEasyConfig).FullName, "TLEasy");
        }

        public void OnDisplayContextMenu(IAgUiPluginMenuBuilder MenuBuilder)
        {
            // Has to be here for inteface
        }

        public void OnInitializeToolbar(IAgUiPluginToolbarBuilder ToolbarBuilder)
        {
            object imageObj = GetImageFromResource("TLEasyPlugin.tleasy.png");
            ToolbarBuilder.AddButton("RealmOne.TLEasyPlugin.OpenTLEasy", "Open TLEasy", "Open TLEasy Plugin Toolbar Button", AgEToolBarButtonOptions.eToolBarButtonOptionAlwaysOn, (stdole.IPictureDisp)imageObj);
        }

        private object GetImageFromResource(string resourceName)
        {
            try
            {
                using (var stream = this.GetType().Assembly.GetManifestResourceStream(resourceName))
                {
                    if (stream != null)
                    {
                        System.Drawing.Image image = System.Drawing.Image.FromStream(stream);
                        return ImageToPictureDispConverter.Convert(image);
                    }
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine("Failed to load toolbar icon: " + ex.Message);
            }
            return null;
        }

        // Internal helper to convert System.Drawing.Image to IPictureDisp
        private class ImageToPictureDispConverter : System.Windows.Forms.AxHost
        {
            private ImageToPictureDispConverter() : base(null) { }
            public static object Convert(System.Drawing.Image image)
            {
                return GetIPictureDispFromPicture(image);
            }
        }

        public AgEUiPluginCommandState QueryState(string CommandName)
        {
            if (string.Compare(CommandName, "RealmOne.TLEasyPlugin.OpenTLEasy", true) == 0)
            {
                return AgEUiPluginCommandState.eUiPluginCommandStateEnabled | AgEUiPluginCommandState.eUiPluginCommandStateSupported;
            }
            return AgEUiPluginCommandState.eUiPluginCommandStateNone;
        }

        public void Exec(string CommandName, IAgProgressTrackCancel TrackCancel, IAgUiPluginCommandParameters Parameters)
        {
            if (string.Compare(CommandName, "RealmOne.TLEasyPlugin.OpenTLEasy", true) == 0)
            {
                OpenUserInterface();
            }
        }

        public void OpenUserInterface()
        {
            EnsureScenarioExists();

            IAgUiPluginWindowSite windows = m_psite as IAgUiPluginWindowSite;

            if (windows == null)
            {
                MessageBox.Show("Host application is unable to open windows.");
            }
            else
            {
                IAgUiPluginWindowCreateParameters winParams = windows.CreateParameters();
                winParams.AllowMultiple = false;
                winParams.AssemblyPath = this.GetType().Assembly.Location;
                winParams.UserControlFullName = typeof(TLEasyPluginControl).FullName;
                winParams.Caption = "TLEasy";
                winParams.DockStyle = AgEDockStyle.eDockStyleFloating;
                winParams.Width = 320;
                winParams.Height = 188;
                object obj = windows.CreateNetToolWindowParam(this, winParams);
            }
        }

        private const string REGISTRY_KEY = "Software\\RealmOne\\TLEasyPlugin";

        private void LoadSettings()
        {
            try
            {
                using (RegistryKey key = Registry.CurrentUser.OpenSubKey(REGISTRY_KEY))
                {
                    if (key != null)
                    {
                        this.tleFilePath = key.GetValue("tleFilePath", "").ToString();
                        this.ScenarioSaveFilePath = key.GetValue("ScenarioSaveFilePath", "").ToString();
                        this.LabelOldData = Convert.ToBoolean(key.GetValue("LabelOldData", false));
                        this.FilterOldAccesses = Convert.ToBoolean(key.GetValue("FilterOldAccesses", false));
                        this.MinAccessTimeSeconds = Convert.ToInt32(key.GetValue("MinAccessTimeSeconds", 420));
                    }
                }
            }
            catch { /* Ignore registry errors */ }
        }

        public void SaveSettings()
        {
            try
            {
                using (RegistryKey key = Registry.CurrentUser.CreateSubKey(REGISTRY_KEY))
                {
                    if (key != null)
                    {
                        key.SetValue("tleFilePath", this.tleFilePath ?? "");
                        key.SetValue("ScenarioSaveFilePath", this.ScenarioSaveFilePath ?? "");
                        key.SetValue("LabelOldData", this.LabelOldData);
                        key.SetValue("FilterOldAccesses", this.FilterOldAccesses);
                        key.SetValue("MinAccessTimeSeconds", this.MinAccessTimeSeconds);
                    }
                }
            }
            catch { /* Ignore registry errors */ }
        }

        public void EnsureScenarioExists()
        {
            // 1. Check if scenario exists using Connect
            string checkResult = SendConCommand("CheckScenario /");

            if (checkResult.Trim() == "1")
            {
                // Scenario exists, do nothing
                return;
            }

            // 2. Scenario does not exist, try to load default
            string scenarioPath = this.ScenarioSaveFilePath;
            bool loaded = false;

            if (!string.IsNullOrEmpty(scenarioPath) && System.IO.File.Exists(scenarioPath))
            {
                try
                {
                    // Use Connect command to load
                    string result = SendConCommand($"Load / Scenario \"{scenarioPath}\"");
                    if (!result.Contains("Ack NAb")) // Simple check for success
                    {
                        loaded = true;
                    }
                }
                catch (Exception ex)
                {
                    MessageBox.Show($"Failed to load default scenario: {ex.Message}. Creating a new one instead.");
                }
            }

            // 3. If not loaded, create new
            if (!loaded)
            {
                SendConCommand("New / Scenario TLEasy");
            }
        }

        private string SendConCommand(string command)
        {
            try
            {
                IAgExecCmdResult result = m_root.ExecuteCommand(command);
                if (result.Count > 0) return result[0].ToString();
                return "";
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"Command Failed: {command} | {ex.Message}");
                return "Ack NAb";
            }
        }
    }
}