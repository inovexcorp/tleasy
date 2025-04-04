package com.realmone.tleasy;

import com.realmone.tleasy.ui.DisableButtonDocumentListener;
import com.realmone.tleasy.ui.InputValidator;
import com.realmone.tleasy.ui.NotNullInputValidator;
import com.realmone.tleasy.ui.RegexInputValidator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;
import java.util.stream.Stream;

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
    private final JCheckBox darkThemeCheckBox;

    public ConfigSetup(boolean exitOnClose) {
        setTitle("Initial Configuration Setup");
        setModal(true);
        setSize(600, 250);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Fields for input
        tleEndpointField = new JTextField();
        keystoreField = new JTextField();
        keystorePassField = new JPasswordField();
        skipCertValidationCheckBox = new JCheckBox("Skip SSL Certificate Validation");
        darkThemeCheckBox = new JCheckBox("Enable Dark Theme");

        // Panel for Styling using vertical BoxLayout for each row
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Row 1: TLE Data Endpoint
        JPanel endpointRow = new JPanel(new BorderLayout(5, 5));
        endpointRow.add(new JLabel("TLE Data Endpoint:"), BorderLayout.WEST);
        endpointRow.add(tleEndpointField, BorderLayout.CENTER);
        panel.add(endpointRow);

        // Row 2: Keystore File Path with Browse button
        JPanel keystoreRow = new JPanel(new BorderLayout(5, 5));
        keystoreRow.add(new JLabel("Keystore File Path:"), BorderLayout.WEST);
        JPanel keystoreInputPanel = new JPanel(new BorderLayout(5, 5));
        keystoreInputPanel.add(keystoreField, BorderLayout.CENTER);
        JButton keystoreBrowseButton = new JButton("Browse...");
        keystoreBrowseButton.addActionListener(e -> chooseFile(keystoreField));
        keystoreInputPanel.add(keystoreBrowseButton, BorderLayout.EAST);
        keystoreRow.add(keystoreInputPanel, BorderLayout.CENTER);
        panel.add(keystoreRow);

        // Row 3: Keystore Password
        JPanel passwordRow = new JPanel(new BorderLayout(5, 5));
        passwordRow.add(new JLabel("Keystore Password:"), BorderLayout.WEST);
        passwordRow.add(keystorePassField, BorderLayout.CENTER);
        panel.add(passwordRow);

        // Row 4: Skip SSL Certificate Validation
        JPanel skipCertRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        skipCertRow.add(skipCertValidationCheckBox);
        panel.add(skipCertRow);

        // Row 5: Dark Theme
        darkThemeCheckBox.setSelected(Configuration.isDarkTheme());
        JPanel darkThemeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        darkThemeRow.add(darkThemeCheckBox);
        panel.add(darkThemeRow);

        // Row 6: Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = new JButton("Save");
        saveButton.setEnabled(false);
        saveButton.addActionListener(new SaveButtonListener());
        buttonRow.add(saveButton);
        JButton cancelButton = new JButton("Cancel" + (exitOnClose ? " & Quit" : ""));
        cancelButton.addActionListener(e -> {
            dispose();
            if (exitOnClose) {
                System.exit(1);
            }
        });
        buttonRow.add(cancelButton);
        panel.add(buttonRow);

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

        // Apply dark theme if enabled
        if (Configuration.isDarkTheme()) {
            applyDarkTheme();
        }

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
        newConfiguration.setProperty(Configuration.PROP_DARK_THEME, String.valueOf(darkThemeCheckBox.isSelected()));
        return newConfiguration;
    }

    /**
     * Applies dark theme styling to the configuration dialog by updating background and foreground colors.
     */
    private void applyDarkTheme() {
        // Set the background of the content pane
        getContentPane().setBackground(java.awt.Color.DARK_GRAY);
        // Recursively update all child components
        applyDarkThemeToComponent(getContentPane());
    }

    /**
     * Recursively applies dark theme styling to a component and its children.
     *
     * @param comp The component to style
     */
    private void applyDarkThemeToComponent(java.awt.Component comp) {
        if (comp instanceof JPanel) {
            comp.setBackground(java.awt.Color.DARK_GRAY);
            for (java.awt.Component child : ((JPanel) comp).getComponents()) {
                applyDarkThemeToComponent(child);
            }
        } else if (comp instanceof JLabel) {
            comp.setForeground(java.awt.Color.WHITE);
        } else if (comp instanceof JTextField || comp instanceof JPasswordField) {
            comp.setBackground(java.awt.Color.GRAY);
            comp.setForeground(java.awt.Color.WHITE);
        } else if (comp instanceof JButton) {
            comp.setBackground(java.awt.Color.GRAY);
            comp.setForeground(java.awt.Color.WHITE);
        } else if (comp instanceof JCheckBox) {
            comp.setBackground(java.awt.Color.DARK_GRAY);
            comp.setForeground(java.awt.Color.WHITE);
        } else {
            // For any other component, apply default dark theme colors
            comp.setBackground(java.awt.Color.DARK_GRAY);
            comp.setForeground(java.awt.Color.WHITE);
        }
    }
}