package com.realmone.tleasy;

import com.realmone.tleasy.ui.RegexDocumentListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

public class ConfigSetup extends JDialog {

    private static final String HTTP_URL_REGEX =
            "^(https?://)" +                      // Protocol: http:// or https://
                    "((([a-zA-Z0-9.-]+)\\.([a-zA-Z]{2,})|localhost|\\d{1,3}(\\.\\d{1,3}){3}))" +  // Domain, Localhost, or IP
                    "(:\\d{1,5})?" +                       // Optional port (e.g., :8080)
                    "(/[^\\s]*)?$";                        // Optional path, query params, fragment

    private final JTextField tleEndpointField;
    private final JTextField keystoreField;
    private final JPasswordField keystorePassField;
    private final JTextField truststoreField;
    private final JPasswordField truststorePassField;
    private final JCheckBox skipCertValidationCheckBox;

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
        saveButton.setEnabled(false);
        saveButton.addActionListener(new SaveButtonListener());
        add(saveButton);

        JButton cancelButton = new JButton("Cancel & Quit");
        cancelButton.addActionListener(e -> {
            dispose();
            // Canceling configuration will prevent success in the next step
            System.exit(1);
        });
        add(cancelButton);

        // Validate configuration of the tle endpoint as a valid URL.
        tleEndpointField.getDocument().addDocumentListener(
                RegexDocumentListener.builder()
                        .button(saveButton)
                        .textField(tleEndpointField)
                        .pattern(HTTP_URL_REGEX)
                        .build());

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private class SaveButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Properties newConfiguration = getProperties();
            // Configure the back end properties file in ~
            try {
                Configuration.configure(newConfiguration);
                JOptionPane.showMessageDialog(ConfigSetup.this, "Configuration saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(ConfigSetup.this, "Failed to save configuration: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Properties getProperties() {
        Properties newConfiguration = new Properties();
        newConfiguration.setProperty(Configuration.PROP_TLE_ENDPOINT, tleEndpointField.getText());
        newConfiguration.setProperty(Configuration.PROP_KEYSTORE, keystoreField.getText());
        newConfiguration.setProperty(Configuration.PROP_KEYSTORE_PASS, new String(keystorePassField.getPassword()));
        newConfiguration.setProperty(Configuration.PROP_TRUSTSTORE, truststoreField.getText());
        newConfiguration.setProperty(Configuration.PROP_TRUSTSTORE_PASS, new String(truststorePassField.getPassword()));
        newConfiguration.setProperty(Configuration.PROP_SKIP_CERT_VALIDATE, String.valueOf(skipCertValidationCheckBox.isSelected()));
        return newConfiguration;
    }
}