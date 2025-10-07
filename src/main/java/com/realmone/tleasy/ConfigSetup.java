package com.realmone.tleasy;

import com.realmone.tleasy.ui.DisableButtonDocumentListener;
import com.realmone.tleasy.ui.InputValidator;
import com.realmone.tleasy.ui.NotNullInputValidator;
import com.realmone.tleasy.ui.OrInputValidator;
import com.realmone.tleasy.ui.RegexInputValidator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.JComponent;
import javax.swing.border.TitledBorder;

public class ConfigSetup extends JDialog {

    private static final String DEFAULT_URL = "https://localhost:8443/secure/download";
    private static final String HTTP_URL_REGEX =
            "^(https?://)" +                      // Protocol: http:// or https://
                    "((([a-zA-Z0-9.-]+)\\.([a-zA-Z]{2,})|localhost|\\d{1,3}(\\.\\d{1,3}){3}))" +  // Domain, Localhost, or IP
                    "(:\\d{1,5})?" +                       // Optional port (e.g., :8080)
                    "(/[^\\s]*)?$";                        // Optional path, query params, fragment

    private final JTextField tleEndpointField;
    private final JTextField tleFileField;
    private final JTextField keystoreField;
    private final JPasswordField keystorePassField;
    private final JCheckBox skipCertValidationCheckBox;
    private final JCheckBox darkThemeCheckBox;
    private final JTextField scenarioSaveFileField;
    private final JTextField exeLocationField;
    private final JCheckBox stkAccessReportToggle;
    private final JSpinner minutesSpinner;
    private final JSpinner secondsSpinner;

    public ConfigSetup(boolean exitOnClose) {
        setTitle("Initial Configuration Setup");
        setModal(true);
        setSize(670, 420);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Fields for input
        tleEndpointField = new JTextField();
        tleFileField = new JTextField();
        keystoreField = new JTextField();
        keystorePassField = new JPasswordField();
        skipCertValidationCheckBox = new JCheckBox("Skip SSL Certificate Validation");
        darkThemeCheckBox = new JCheckBox("Enable Dark Theme");
        scenarioSaveFileField = new JTextField();
        exeLocationField = new JTextField();
        stkAccessReportToggle = new JCheckBox("Check for STK Access Report in .csv format, uncheck for .txt format");

        // Minutes can go from 0 to 99, if you want that for some strange reason
        SpinnerModel minutesModel = new SpinnerNumberModel(0, 0, 99, 1);
        minutesSpinner = new JSpinner(minutesModel);
        // Seconds must be between 0 and 59
        SpinnerModel secondsModel = new SpinnerNumberModel(0, 0, 59, 1);
        secondsSpinner = new JSpinner(secondsModel);

        // Set a number format to ensure two digits are always displayed (e.g., "05").
        JSpinner.NumberEditor minutesEditor = new JSpinner.NumberEditor(minutesSpinner, "00");
        minutesSpinner.setEditor(minutesEditor);
        JSpinner.NumberEditor secondsEditor = new JSpinner.NumberEditor(secondsSpinner, "00");
        secondsSpinner.setEditor(secondsEditor);

        JRadioButton tleEndpointRadio = new JRadioButton("Endpoint");
        JRadioButton tleFileRadio = new JRadioButton("File");

        // Group the radio buttons
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(tleEndpointRadio);
        buttonGroup.add(tleFileRadio);

        // Panel for Styling using vertical BoxLayout for each row
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Row 1: TLE Radio Buttons
        JPanel radioButtonRow = new JPanel(new BorderLayout(5, 5));
        radioButtonRow.add(tleEndpointRadio, BorderLayout.WEST);
        radioButtonRow.add(tleFileRadio, BorderLayout.CENTER);
        panel.add(radioButtonRow);

        // Row 2: TLE Data Endpoint
        JPanel endpointRow = new JPanel(new BorderLayout(5, 5));
        endpointRow.add(new JLabel("TLE Data Endpoint:"), BorderLayout.WEST);
        endpointRow.add(tleEndpointField, BorderLayout.CENTER);
        panel.add(endpointRow);

        // Row 3: TLE File Endpoint
        JPanel fileRow = new JPanel(new BorderLayout(5, 5));
        fileRow.add(new JLabel("TLE File Path:"), BorderLayout.WEST);
        JPanel fileInputPanel = new JPanel(new BorderLayout(5, 5));
        fileInputPanel.add(tleFileField, BorderLayout.CENTER);
        JButton fileBrowseButton = new JButton("Browse...");
        fileBrowseButton.addActionListener(e -> chooseFile(tleFileField));
        fileInputPanel.add(fileBrowseButton, BorderLayout.EAST);
        fileRow.add(fileInputPanel, BorderLayout.CENTER);
        panel.add(fileRow);

        // Row 4: Keystore File Path with Browse button
        JPanel keystoreRow = new JPanel(new BorderLayout(5, 5));
        keystoreRow.add(new JLabel("Keystore File Path:"), BorderLayout.WEST);
        JPanel keystoreInputPanel = new JPanel(new BorderLayout(5, 5));
        keystoreInputPanel.add(keystoreField, BorderLayout.CENTER);
        JButton keystoreBrowseButton = new JButton("Browse...");
        keystoreBrowseButton.addActionListener(e -> chooseFile(keystoreField));
        keystoreInputPanel.add(keystoreBrowseButton, BorderLayout.EAST);
        keystoreRow.add(keystoreInputPanel, BorderLayout.CENTER);
        panel.add(keystoreRow);

        // Setup initial visibility of TLE Endpoint and File rows
        endpointRow.setVisible(true);
        fileRow.setVisible(false);

        // Add action listeners to radio buttons
        tleEndpointRadio.addActionListener(e -> {
            endpointRow.setVisible(true);
            fileRow.setVisible(false);
            tleFileField.setText("");
        });
        tleFileRadio.addActionListener(e -> {
            endpointRow.setVisible(false);
            tleEndpointField.setText("");
            fileRow.setVisible(true);
        });

        // Row 5: Keystore Password
        JPanel passwordRow = new JPanel(new BorderLayout(5, 5));
        passwordRow.add(new JLabel("Keystore Password:"), BorderLayout.WEST);
        passwordRow.add(keystorePassField, BorderLayout.CENTER);
        panel.add(passwordRow);

        // Row 6: Skip SSL Certificate Validation
        JPanel skipCertRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        skipCertRow.add(skipCertValidationCheckBox);
        panel.add(skipCertRow);

        // Row 7: Dark Theme
        darkThemeCheckBox.setSelected(Configuration.isDarkTheme());
        JPanel darkThemeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        darkThemeRow.add(darkThemeCheckBox);
        panel.add(darkThemeRow);

        // Create a new panel for STK-related settings with a titled border
        JPanel stkPanel = new JPanel();
        stkPanel.setLayout(new BoxLayout(stkPanel, BoxLayout.Y_AXIS));
        stkPanel.setBorder(BorderFactory.createTitledBorder("STK Configuration"));
        panel.add(stkPanel); // Add the new container panel to the main panel

        // STK Row 1 (Row 8): Scenario Save File Path
        JPanel scenarioSaveFileRow = new JPanel(new BorderLayout(5, 5));
        scenarioSaveFileRow.add(new JLabel("Scenario Save File Path:"), BorderLayout.WEST);
        JPanel scenarioSaveFileInputPanel = new JPanel(new BorderLayout(5, 5));
        scenarioSaveFileInputPanel.add(scenarioSaveFileField, BorderLayout.CENTER);
        JButton scenarioSaveFileBrowseButton = new JButton("Browse...");
        scenarioSaveFileBrowseButton.addActionListener(e -> chooseFile(scenarioSaveFileField));
        scenarioSaveFileInputPanel.add(scenarioSaveFileBrowseButton, BorderLayout.EAST);
        scenarioSaveFileRow.add(scenarioSaveFileInputPanel, BorderLayout.CENTER);
        stkPanel.add(scenarioSaveFileRow);

        // STK Row 1 (Row 9): STK .exe File Path
        JPanel exeLocationRow = new JPanel(new BorderLayout(5, 5));
        exeLocationRow.add(new JLabel("STK File Path:"), BorderLayout.WEST);
        JPanel exeLocationInputPanel = new JPanel(new BorderLayout(5, 5));
        exeLocationInputPanel.add(exeLocationField, BorderLayout.CENTER);
        JButton exeLocationBrowseButton = new JButton("Browse...");
        exeLocationBrowseButton.addActionListener(e -> chooseFile(exeLocationField));
        exeLocationInputPanel.add(exeLocationBrowseButton, BorderLayout.EAST);
        exeLocationRow.add(exeLocationInputPanel, BorderLayout.CENTER);
        stkPanel.add(exeLocationRow);

        // STK Row 2 (Row 10): Toggle STK access report file format
        stkAccessReportToggle.setSelected(Configuration.isCsv());
        JPanel stkAccessReportFileFormatRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stkAccessReportFileFormatRow.add(stkAccessReportToggle);
        stkPanel.add(stkAccessReportFileFormatRow);

        // STK Row 3 (Row 11): Time filter spinners
        JPanel timeFilterRow = new JPanel(new BorderLayout(5, 5));
        timeFilterRow.add(new JLabel("Minimum STK Access Time (MM:ss):"), BorderLayout.WEST);
        JPanel timeSpinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        timeSpinnerPanel.add(minutesSpinner);
        timeSpinnerPanel.add(new JLabel(":"));
        timeSpinnerPanel.add(secondsSpinner);
        timeFilterRow.add(timeSpinnerPanel, BorderLayout.CENTER);
        stkPanel.add(timeFilterRow);

        // Final Row: Buttons
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

        // Validate configuration of the tle endpoint as a valid URL or a tle file is set
        InputValidator endpointRegexValidator = RegexInputValidator.builder()
                .field(tleEndpointField)
                .pattern(HTTP_URL_REGEX)
                .build();
        InputValidator notNullTleFileValidator = NotNullInputValidator.builder().field(tleFileField).build();
        InputValidator endpointOrFileValidator = OrInputValidator.builder()
                .validator(endpointRegexValidator)
                .validator(notNullTleFileValidator)
                .build();
        // Ensure all text inputs are not null/empty
        InputValidator notNullKeystoreValidator = NotNullInputValidator.builder().field(keystoreField).build();
        InputValidator notNullKeystorePassValidator = NotNullInputValidator.builder().field(keystorePassField).build();
        // Create document listener to disable button if any field is invalid
        DisableButtonDocumentListener listener = new DisableButtonDocumentListener(saveButton, endpointOrFileValidator,
                notNullKeystoreValidator, notNullKeystorePassValidator);
        // Add document listener to all fields
        Stream.of(tleEndpointField, tleFileField, keystoreField, keystorePassField).forEach(field ->
                field.getDocument().addDocumentListener(listener));

        // Populate fields with existing configuration
        if (Configuration.getKeyStoreFile() != null) {
            keystoreField.setText(Configuration.getKeyStoreFile().getAbsolutePath());
        }
        if (Configuration.isConfigured()) {
            // Handles the initialization of the TLE Endpoint and File fields depending on what was loaded
            if (!Configuration.getTleDataEndpoint().isEmpty()) {
                tleEndpointField.setText(Configuration.getTleDataEndpoint());
                tleFileField.setText("");
                tleEndpointRadio.setSelected(true);
            } else if (Configuration.getTleFile() != null) {
                tleFileField.setText(Configuration.getTleFile().getAbsolutePath());
                tleEndpointField.setText("");
                endpointRow.setVisible(false);
                fileRow.setVisible(true);
                tleFileRadio.setSelected(true);
            }
            keystorePassField.setText(new String(Configuration.getKeystorePassword()));
            skipCertValidationCheckBox.setSelected(Configuration.isSkipCertificateValidation());
        }
        if (Configuration.getScenarioSaveFile() != null) {
            scenarioSaveFileField.setText(Configuration.getScenarioSaveFile().getAbsolutePath());
        }

        if (Configuration.getExeFile() != null) {
            exeLocationField.setText(Configuration.getExeFile().getAbsolutePath());
        }

        minutesSpinner.setValue(Configuration.getTimeFilterMinutes());
        secondsSpinner.setValue(Configuration.getTimeFilterSeconds());

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
        newConfiguration.setProperty(Configuration.PROP_TLE_FILE, tleFileField.getText());
        newConfiguration.setProperty(Configuration.PROP_KEYSTORE, keystoreField.getText());
        newConfiguration.setProperty(Configuration.PROP_KEYSTORE_PASS, new String(keystorePassField.getPassword()));
        newConfiguration.setProperty(Configuration.PROP_SKIP_CERT_VALIDATE, String.valueOf(skipCertValidationCheckBox.isSelected()));
        newConfiguration.setProperty(Configuration.PROP_DARK_THEME, String.valueOf(darkThemeCheckBox.isSelected()));
        newConfiguration.setProperty(Configuration.PROP_SCENARIO_SAVE_FILE, scenarioSaveFileField.getText());
        newConfiguration.setProperty(Configuration.PROP_EXE_LOCATION, exeLocationField.getText());
        newConfiguration.setProperty(Configuration.PROP_STK_ACCESS_REPORT_FORMAT, String.valueOf(stkAccessReportToggle.isSelected()));
        newConfiguration.setProperty(Configuration.PROP_TIME_FILTER_MINUTES, String.valueOf(minutesSpinner.getValue()));
        newConfiguration.setProperty(Configuration.PROP_TIME_FILTER_SECONDS, String.valueOf(secondsSpinner.getValue()));

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
    private void applyDarkThemeToComponent(Component comp) {
        comp.setBackground(Color.DARK_GRAY);
        comp.setForeground(Color.WHITE);

        if (comp instanceof JPanel || comp instanceof JRadioButton || comp instanceof JCheckBox) {
            // Check if the JPanel has a TitledBorder and set its title color
            if (comp instanceof JPanel && ((JPanel) comp).getBorder() instanceof TitledBorder) {
                TitledBorder titledBorder = (TitledBorder) ((JPanel) comp).getBorder();
                titledBorder.setTitleColor(Color.WHITE);
            }
            // Container components need to have their children styled as well
            for (Component child : ((JComponent) comp).getComponents()) {
                applyDarkThemeToComponent(child);
            }
        } else if (comp instanceof JTextField) {
            comp.setBackground(Color.GRAY);
            comp.setForeground(Color.WHITE);
        } else if (comp instanceof JButton) {
            comp.setBackground(Color.GRAY);
            comp.setForeground(Color.DARK_GRAY);
        } else if (comp instanceof JSpinner) {
            JComponent editor = ((JSpinner) comp).getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
                textField.setBackground(Color.GRAY);
                textField.setForeground(Color.WHITE);
            }
            for (Component c : ((Container) comp).getComponents()) {
                if (c instanceof JButton) {
                    c.setBackground(Color.GRAY);
                    c.setForeground(Color.DARK_GRAY);
                }
            }
        }
    }
}