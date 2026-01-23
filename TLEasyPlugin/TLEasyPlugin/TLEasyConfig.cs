using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Drawing;
using System.Data;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Runtime.InteropServices;
using AGI.Ui.Plugins;
using TLEasyPlugin.Util;

namespace TLEasyPlugin
{
    [Guid("5C3B6155-7ED6-40F1-9238-77C184B42555")]
    [ProgId("RealmOne.TLEasyConfig")]
    [ClassInterface(ClassInterfaceType.None)]
    public partial class TLEasyConfig : UserControl, IAgUiPluginConfigurationPageActions
    {
        IAgUiPluginConfigurationPageSite m_site;
        TLEasyPlugin m_plugin;

        public TLEasyConfig()
        {
            InitializeComponent();
        }

        public void OnCreated(IAgUiPluginConfigurationPageSite Site)
        {
            m_site = Site;
            m_plugin = m_site.Plugin as TLEasyPlugin;

            if (m_plugin != null)
            {
                // 1. Load File Path
                if (tleFilePathTextBox != null)
                    tleFilePathTextBox.Text = m_plugin.tleFilePath;

                // 2. Load Checkboxes
                if (chkLabelOld != null)
                    chkLabelOld.Checked = m_plugin.LabelOldData;

                if (chkFilterOld != null)
                    chkFilterOld.Checked = m_plugin.FilterOldAccesses;

                // 3. Load Time (Convert Total Seconds back to Minutes/Seconds)
                if (numMin != null && numSec != null)
                {
                    numMin.Value = m_plugin.MinAccessTimeSeconds / 60; // Integer division gets minutes
                    numSec.Value = m_plugin.MinAccessTimeSeconds % 60; // Modulo gets remaining seconds
                }

                // 4. Load Scenario Path
                if (txtScenarioPath != null)
                    txtScenarioPath.Text = m_plugin.ScenarioSaveFilePath;
            }
        }

        private void browseButton_Click(object sender, EventArgs e)
        {
            OpenFileDialog openFileDialog = new OpenFileDialog();
            openFileDialog.Filter = "TLE Files (*.tle;*.txt)|*.tle;*.txt|All files (*.*)|*.*";
            openFileDialog.Title = "Select TLE File";

            if (openFileDialog.ShowDialog() == DialogResult.OK)
            {
                tleFilePathTextBox.Text = openFileDialog.FileName;
            }
        }

        private void btnBrowseScenario_Click(object sender, EventArgs e)
        {
            OpenFileDialog openFileDialog = new OpenFileDialog();
            openFileDialog.Filter = "STK Scenario (*.sc)|*.sc|All files (*.*)|*.*";
            openFileDialog.Title = "Select Default Scenario Path";

            if (openFileDialog.ShowDialog() == DialogResult.OK)
            {
                txtScenarioPath.Text = openFileDialog.FileName;
            }
        }

        public void OnOK()
        {
            SaveSettings();
        }

        public bool OnApply()
        {
            SaveSettings();
            return true;
        }

        private void SaveSettings()
        {
            if (m_plugin != null)
            {
                // Save Text Box
                m_plugin.tleFilePath = tleFilePathTextBox.Text;

                // Save Checkboxes
                m_plugin.LabelOldData = chkLabelOld.Checked;
                m_plugin.FilterOldAccesses = chkFilterOld.Checked;

                // Save Time (Convert Minutes/Seconds back to Total Seconds)
                m_plugin.MinAccessTimeSeconds = (int)((numMin.Value * 60) + numSec.Value);

                // Save Scenario Path
                m_plugin.ScenarioSaveFilePath = txtScenarioPath.Text;

                // Persist to Registry immediately
                m_plugin.SaveSettings();
            }
        }

        public void OnCancel()
        {
            // Do nothing
        }

    }
}