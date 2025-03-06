package com.realmone.tleasy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

public class ConfigSetup extends JDialog {

    private JTextField tleEndpointField;
    private JTextField keystoreField;
    private JPasswordField keystorePassField;
    private JTextField truststoreField;
    private JPasswordField truststorePassField;
    private JCheckBox skipCertValidationCheckBox;

    public ConfigSetup() {
        setTitle("Initial Configuration Setup");
        setLayout(new GridLayout(7, 2));
        setModal(true);
        setSize(500, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Fields for input
        tleEndpointField = new JTextField();
        keystoreField = new JTextField();
        keystorePassField = new JPasswordField();
        truststoreField = new JTextField();
        truststorePassField = new JPasswordField();
        skipCertValidationCheckBox = new JCheckBox("Skip SSL Certificate Validation");

        // Labels
        add(new JLabel("TLE Data Endpoint:"));
        add(tleEndpointField);

        add(new JLabel("Keystore File Path:"));
        add(keystoreField);

        add(new JLabel("Keystore Password:"));
        add(keystorePassField);

        add(new JLabel("Truststore File Path:"));
        add(truststoreField);

        add(new JLabel("Truststore Password:"));
        add(truststorePassField);

        add(new JLabel("Skip Cert Validation:"));
        add(skipCertValidationCheckBox);

        // Buttons
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new SaveButtonListener());
        add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        add(cancelButton);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private class SaveButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Properties config = new Properties();
            config.setProperty(Configuration.PROP_TLE_ENDPOINT, tleEndpointField.getText());
            config.setProperty(Configuration.PROP_KEYSTORE, keystoreField.getText());
            config.setProperty(Configuration.PROP_KEYSTORE_PASS, new String(keystorePassField.getPassword()));
            config.setProperty(Configuration.PROP_TRUSTSTORE, truststoreField.getText());
            config.setProperty(Configuration.PROP_TRUSTSTORE_PASS, new String(truststorePassField.getPassword()));
            config.setProperty(Configuration.PROP_SKIP_CERT_VALIDATE, String.valueOf(skipCertValidationCheckBox.isSelected()));

            try {
                Configuration.configure(config);
                JOptionPane.showMessageDialog(ConfigSetup.this, "Configuration saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(ConfigSetup.this, "Failed to save configuration: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}