namespace TLEasyPlugin
{
    partial class TLEasyConfig
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Component Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify 
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.txtboxDefaultFilePath = new System.Windows.Forms.TextBox();
            this.tleFilePathTextBox = new System.Windows.Forms.TextBox();
            this.browseButton = new System.Windows.Forms.Button();
            this.label1 = new System.Windows.Forms.Label();
            this.chkLabelOld = new System.Windows.Forms.CheckBox();
            this.chkFilterOld = new System.Windows.Forms.CheckBox();
            this.label2 = new System.Windows.Forms.Label();
            this.numMin = new System.Windows.Forms.NumericUpDown();
            this.numSec = new System.Windows.Forms.NumericUpDown();
            this.label3 = new System.Windows.Forms.Label();
            this.txtScenarioPath = new System.Windows.Forms.TextBox();
            this.btnBrowseScenario = new System.Windows.Forms.Button();
            ((System.ComponentModel.ISupportInitialize)(this.numMin)).BeginInit();
            ((System.ComponentModel.ISupportInitialize)(this.numSec)).BeginInit();
            this.SuspendLayout();
            // 
            // txtboxDefaultFilePath
            // 
            this.txtboxDefaultFilePath.Location = new System.Drawing.Point(0, 0);
            this.txtboxDefaultFilePath.Name = "txtboxDefaultFilePath";
            this.txtboxDefaultFilePath.Size = new System.Drawing.Size(100, 22);
            this.txtboxDefaultFilePath.TabIndex = 0;
            // 
            // tleFilePathTextBox
            // 
            this.tleFilePathTextBox.Location = new System.Drawing.Point(128, 3);
            this.tleFilePathTextBox.Name = "tleFilePathTextBox";
            this.tleFilePathTextBox.Size = new System.Drawing.Size(312, 22);
            this.tleFilePathTextBox.TabIndex = 0;
            // 
            // browseButton
            // 
            this.browseButton.Location = new System.Drawing.Point(446, 2);
            this.browseButton.Name = "browseButton";
            this.browseButton.Size = new System.Drawing.Size(75, 23);
            this.browseButton.TabIndex = 1;
            this.browseButton.Text = "Browse";
            this.browseButton.UseVisualStyleBackColor = true;
            this.browseButton.Click += new System.EventHandler(this.browseButton_Click);
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(11, 6);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(114, 16);
            this.label1.TabIndex = 2;
            this.label1.Text = "TLE File Location:";
            // 
            // chkLabelOld
            // 
            this.chkLabelOld.AutoSize = true;
            this.chkLabelOld.Location = new System.Drawing.Point(14, 31);
            this.chkLabelOld.Name = "chkLabelOld";
            this.chkLabelOld.Size = new System.Drawing.Size(208, 20);
            this.chkLabelOld.TabIndex = 3;
            this.chkLabelOld.Text = "Label data older than 24 hours";
            this.chkLabelOld.UseVisualStyleBackColor = true;
            // 
            // chkFilterOld
            // 
            this.chkFilterOld.AutoSize = true;
            this.chkFilterOld.Location = new System.Drawing.Point(14, 57);
            this.chkFilterOld.Name = "chkFilterOld";
            this.chkFilterOld.Size = new System.Drawing.Size(195, 20);
            this.chkFilterOld.TabIndex = 4;
            this.chkFilterOld.Text = "Filter out 24-hour+ accesses";
            this.chkFilterOld.UseVisualStyleBackColor = true;
            // 
            // label2
            // 
            this.label2.AutoSize = true;
            this.label2.Location = new System.Drawing.Point(11, 86);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(240, 16);
            this.label2.TabIndex = 5;
            this.label2.Text = "Minimum Facility Access Time (MM:ss):";
            // 
            // numMin
            // 
            this.numMin.Location = new System.Drawing.Point(257, 84);
            this.numMin.Maximum = new decimal(new int[] {
            999,
            0,
            0,
            0});
            this.numMin.Name = "numMin";
            this.numMin.Size = new System.Drawing.Size(52, 22);
            this.numMin.TabIndex = 6;
            // 
            // numSec
            // 
            this.numSec.Location = new System.Drawing.Point(315, 84);
            this.numSec.Maximum = new decimal(new int[] {
            59,
            0,
            0,
            0});
            this.numSec.Name = "numSec";
            this.numSec.Size = new System.Drawing.Size(53, 22);
            this.numSec.TabIndex = 7;
            // 
            // label3
            // 
            this.label3.AutoSize = true;
            this.label3.Location = new System.Drawing.Point(11, 113);
            this.label3.Name = "label3";
            this.label3.Size = new System.Drawing.Size(139, 16);
            this.label3.TabIndex = 8;
            this.label3.Text = "Default Scenario Path:";
            // 
            // txtScenarioPath
            // 
            this.txtScenarioPath.Location = new System.Drawing.Point(150, 110);
            this.txtScenarioPath.Name = "txtScenarioPath";
            this.txtScenarioPath.Size = new System.Drawing.Size(290, 22);
            this.txtScenarioPath.TabIndex = 9;
            // 
            // btnBrowseScenario
            // 
            this.btnBrowseScenario.Location = new System.Drawing.Point(446, 109);
            this.btnBrowseScenario.Name = "btnBrowseScenario";
            this.btnBrowseScenario.Size = new System.Drawing.Size(75, 23);
            this.btnBrowseScenario.TabIndex = 10;
            this.btnBrowseScenario.Text = "Browse";
            this.btnBrowseScenario.UseVisualStyleBackColor = true;
            this.btnBrowseScenario.Click += new System.EventHandler(this.btnBrowseScenario_Click);
            // 
            // TLEasyConfig
            // 
            this.Controls.Add(this.btnBrowseScenario);
            this.Controls.Add(this.txtScenarioPath);
            this.Controls.Add(this.label3);
            this.Controls.Add(this.numSec);
            this.Controls.Add(this.numMin);
            this.Controls.Add(this.label2);
            this.Controls.Add(this.chkFilterOld);
            this.Controls.Add(this.chkLabelOld);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.browseButton);
            this.Controls.Add(this.tleFilePathTextBox);
            this.Name = "TLEasyConfig";
            this.Size = new System.Drawing.Size(815, 351);
            ((System.ComponentModel.ISupportInitialize)(this.numMin)).EndInit();
            ((System.ComponentModel.ISupportInitialize)(this.numSec)).EndInit();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.TextBox txtboxDefaultFilePath;
        private System.Windows.Forms.TextBox tleFilePathTextBox;
        private System.Windows.Forms.Button browseButton;
        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.CheckBox chkLabelOld;
        private System.Windows.Forms.CheckBox chkFilterOld;
        private System.Windows.Forms.Label label2;
        private System.Windows.Forms.NumericUpDown numMin;
        private System.Windows.Forms.NumericUpDown numSec;
        private System.Windows.Forms.Label label3;
        private System.Windows.Forms.TextBox txtScenarioPath;
        private System.Windows.Forms.Button btnBrowseScenario;
    }
}
