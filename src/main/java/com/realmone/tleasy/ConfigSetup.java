package com.realmone.tleasy;

import com.realmone.tleasy.ui.DisableButtonDocumentListener;
import com.realmone.tleasy.ui.InputValidator;
import com.realmone.tleasy.ui.NotNullInputValidator;
import com.realmone.tleasy.ui.RegexInputValidator;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class ConfigSetup extends JDialog {

    private static final String HTTP_URL_REGEX =
            "^(https?://)" +                      // Protocol: http:// or https://
                    "((([a-zA-Z0-9.-]+)\\.([a-zA-Z]{2,})|localhost|\\d{1,3}(\\.\\d{1,3}){3}))" +  // Domain, Localhost, or IP
                    "(:\\d{1,5})?" +                       // Optional port (e.g., :8080)
                    "(/[^\\s]*)?$";                        // Optional path, query params, fragment

    private final JTextField tleEndpointField;
    private final JTextField keystoreField;
    private final JPasswordField keystorePassField;
    private final JCheckBox skipCertValidationCheckBox;

    public ConfigSetup(boolean exitOnClose) {
        setTitle("Initial Configuration Setup");
        setModal(true);
        setSize(600, 200);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Panel for Styling
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 3)); // Adjusted to accommodate file chooser buttons
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Adds padding around the form


        // Fields for input
        tleEndpointField = new JTextField();
        keystoreField = new JTextField();
        keystorePassField = new JPasswordField();
        skipCertValidationCheckBox = new JCheckBox("Skip SSL Certificate Validation");

        // Add labels and fields
        panel.add(new JLabel("TLE Data Endpoint:"));
        panel.add(tleEndpointField);
        panel.add(new JLabel()); // Empty cell for layout alignment

        panel.add(new JLabel("Keystore File Path:"));
        panel.add(keystoreField);
        JButton keystoreBrowseButton = new JButton("Browse...");
        keystoreBrowseButton.addActionListener(e -> chooseFile(keystoreField));
        panel.add(keystoreBrowseButton);

        panel.add(new JLabel("Keystore Password:"));
        panel.add(keystorePassField);
        panel.add(new JLabel()); // Empty cell for layout alignment

        panel.add(new JLabel());
        panel.add(skipCertValidationCheckBox);
        panel.add(new JLabel()); // Empty cell for layout alignment

        panel.add(new JLabel()); // Empty cell for layout alignment

        // Buttons
        JButton saveButton = new JButton("Save");
        saveButton.setEnabled(false);
        saveButton.addActionListener(new SaveButtonListener());
        panel.add(saveButton);

        JButton cancelButton = new JButton("Cancel" + (exitOnClose ? " & Quit" : ""));
        cancelButton.addActionListener(e -> {
            dispose();
            if (exitOnClose) {
                System.exit(1);
            }
        });
        panel.add(cancelButton);

        // Validate configuration of the tle endpoint as a valid URL.
        InputValidator endpointRegexValidator = RegexInputValidator.builder()
                .field(tleEndpointField)
                .pattern(HTTP_URL_REGEX)
                .build();
        // Ensure all text inputs are not null/empty
        InputValidator notNullKeystoreValidator = NotNullInputValidator.builder().field(keystoreField).build();
        InputValidator notNullKeystorePassValidator = NotNullInputValidator.builder().field(keystorePassField).build();
        // Create document listener to disable button if any field is invalid
        DisableButtonDocumentListener listener = new DisableButtonDocumentListener(saveButton, endpointRegexValidator,
                notNullKeystoreValidator, notNullKeystorePassValidator);
        // Add document listener to all fields
        Stream.of(tleEndpointField, keystoreField, keystorePassField).forEach(field -> {
            field.getDocument().addDocumentListener(listener);
        });

        // Populate fields with existing configuration
        if (Configuration.getKeyStoreFile() != null) {
            keystoreField.setText(Configuration.getKeyStoreFile().getAbsolutePath());
        }
        if (Configuration.isConfigured()) {
            tleEndpointField.setText(Configuration.getTleDataEndpoint());
            keystorePassField.setText(new String(Configuration.getKeystorePassword()));
            skipCertValidationCheckBox.setSelected(Configuration.isSkipCertificateValidation());
        }

        // Add panel to dialog
        add(panel);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Default constructor which will not exit the program on close.
     */
    public ConfigSetup() {
        this(false);
    }

    /**
     * Opens a {@link JFileChooser} for selecting the file whose absolute path will be used to populate the provided
     * target field.
     *
     * @param targetField The {@link JTextField} that will be populated with the selected file's absolute path.
     */
    private void chooseFile(JTextField targetField) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            targetField.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * The {@link ActionListener} used for reacting to the save button being pressed. Stores the entered configuration
     * properties and closes the dialog.
     */
    private class SaveButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Properties newConfiguration = getProperties();
            try {
                Configuration.configure(newConfiguration);
                JOptionPane.showMessageDialog(ConfigSetup.this, "Configuration saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(ConfigSetup.this, "Failed to save configuration: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Creates a {@link Properties} object representing the entered configuration from the form.
     *
     * @return A {@link Properties} object with all the TLE configuration parameters.
     */
    private Properties getProperties() {
        Properties newConfiguration = new Properties();
        newConfiguration.setProperty(Configuration.PROP_TLE_ENDPOINT, tleEndpointField.getText());
        newConfiguration.setProperty(Configuration.PROP_KEYSTORE, keystoreField.getText());
        newConfiguration.setProperty(Configuration.PROP_KEYSTORE_PASS, new String(keystorePassField.getPassword()));
        newConfiguration.setProperty(Configuration.PROP_SKIP_CERT_VALIDATE, String.valueOf(skipCertValidationCheckBox.isSelected()));
        return newConfiguration;
    }
}