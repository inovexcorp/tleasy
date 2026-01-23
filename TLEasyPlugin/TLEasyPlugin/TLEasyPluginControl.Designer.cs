namespace TLEasyPlugin
{
    partial class TLEasyPluginControl
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
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(TLEasyPluginControl));
            this.label1 = new System.Windows.Forms.Label();
            this.txtIDs = new System.Windows.Forms.TextBox();
            this.btnLoadSTK = new System.Windows.Forms.Button();
            this.lblStatus = new System.Windows.Forms.Label();
            this.label2 = new System.Windows.Forms.Label();
            this.pictureBoxLeft = new System.Windows.Forms.PictureBox();
            this.pictureBoxRight = new System.Windows.Forms.PictureBox();
            this.lstSuggestions = new System.Windows.Forms.ListBox();
            this.label3 = new System.Windows.Forms.Label();
            ((System.ComponentModel.ISupportInitialize)(this.pictureBoxLeft)).BeginInit();
            ((System.ComponentModel.ISupportInitialize)(this.pictureBoxRight)).BeginInit();
            this.SuspendLayout();
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(39, 0);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(224, 16);
            this.label1.TabIndex = 0;
            this.label1.Text = "Enter list of 5-digit ID(s) and/or range:";
            // 
            // txtIDs
            // 
            this.txtIDs.Location = new System.Drawing.Point(42, 19);
            this.txtIDs.Name = "txtIDs";
            this.txtIDs.Size = new System.Drawing.Size(221, 22);
            this.txtIDs.TabIndex = 1;
            // 
            // btnLoadSTK
            // 
            this.btnLoadSTK.Location = new System.Drawing.Point(42, 72);
            this.btnLoadSTK.Name = "btnLoadSTK";
            this.btnLoadSTK.Size = new System.Drawing.Size(221, 23);
            this.btnLoadSTK.TabIndex = 2;
            this.btnLoadSTK.Text = "Load into STK";
            this.btnLoadSTK.UseVisualStyleBackColor = true;
            this.btnLoadSTK.Click += new System.EventHandler(this.btnLoadSTK_Click);
            // 
            // lblStatus
            // 
            this.lblStatus.AutoSize = true;
            this.lblStatus.Location = new System.Drawing.Point(127, 98);
            this.lblStatus.Name = "lblStatus";
            this.lblStatus.Size = new System.Drawing.Size(48, 16);
            this.lblStatus.TabIndex = 3;
            this.lblStatus.Text = "Ready";
            // 
            // label2
            // 
            this.label2.AutoSize = true;
            this.label2.ForeColor = System.Drawing.SystemColors.ControlDarkDark;
            this.label2.Location = new System.Drawing.Point(11, 48);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(298, 16);
            this.label2.TabIndex = 4;
            this.label2.Text = "Separate with comma and hyphenate for a range.";
            // 
            // pictureBoxLeft
            // 
            this.pictureBoxLeft.Image = ((System.Drawing.Image)(resources.GetObject("pictureBoxLeft.Image")));
            this.pictureBoxLeft.Location = new System.Drawing.Point(0, 5);
            this.pictureBoxLeft.Name = "pictureBoxLeft";
            this.pictureBoxLeft.Size = new System.Drawing.Size(40, 40);
            this.pictureBoxLeft.SizeMode = System.Windows.Forms.PictureBoxSizeMode.Zoom;
            this.pictureBoxLeft.TabIndex = 5;
            this.pictureBoxLeft.TabStop = false;
            // 
            // pictureBoxRight
            // 
            this.pictureBoxRight.Image = ((System.Drawing.Image)(resources.GetObject("pictureBoxRight.Image")));
            this.pictureBoxRight.Location = new System.Drawing.Point(269, 5);
            this.pictureBoxRight.Name = "pictureBoxRight";
            this.pictureBoxRight.Size = new System.Drawing.Size(40, 40);
            this.pictureBoxRight.SizeMode = System.Windows.Forms.PictureBoxSizeMode.Zoom;
            this.pictureBoxRight.TabIndex = 6;
            this.pictureBoxRight.TabStop = false;
            // 
            // lstSuggestions
            // 
            this.lstSuggestions.FormattingEnabled = true;
            this.lstSuggestions.ItemHeight = 16;
            this.lstSuggestions.Location = new System.Drawing.Point(42, 41);
            this.lstSuggestions.Name = "lstSuggestions";
            this.lstSuggestions.Size = new System.Drawing.Size(221, 116);
            this.lstSuggestions.TabIndex = 10;
            this.lstSuggestions.Visible = false;
            // 
            // label3
            // 
            this.label3.AutoSize = true;
            this.label3.Font = new System.Drawing.Font("Microsoft Sans Serif", 6F);
            this.label3.ForeColor = System.Drawing.SystemColors.ControlDark;
            this.label3.Location = new System.Drawing.Point(72, 160);
            this.label3.Name = "label3";
            this.label3.Size = new System.Drawing.Size(156, 13);
            this.label3.TabIndex = 11;
            this.label3.Text = "Powered by RealmOne Foundry";
            // 
            // TLEasyPluginControl
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(8F, 16F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.Controls.Add(this.label3);
            this.Controls.Add(this.lstSuggestions);
            this.Controls.Add(this.pictureBoxRight);
            this.Controls.Add(this.pictureBoxLeft);
            this.Controls.Add(this.label2);
            this.Controls.Add(this.lblStatus);
            this.Controls.Add(this.btnLoadSTK);
            this.Controls.Add(this.txtIDs);
            this.Controls.Add(this.label1);
            this.Name = "TLEasyPluginControl";
            this.Size = new System.Drawing.Size(312, 193);
            ((System.ComponentModel.ISupportInitialize)(this.pictureBoxLeft)).EndInit();
            ((System.ComponentModel.ISupportInitialize)(this.pictureBoxRight)).EndInit();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.TextBox txtIDs;
        private System.Windows.Forms.Button btnLoadSTK;
        private System.Windows.Forms.Label lblStatus;
        private System.Windows.Forms.Label label2;
        private System.Windows.Forms.PictureBox pictureBoxLeft;
        private System.Windows.Forms.PictureBox pictureBoxRight;
        private System.Windows.Forms.ListBox lstSuggestions;
        private System.Windows.Forms.Label label3;
    }
}
