package com.realmone.tleasy;

import com.realmone.tleasy.rest.SimpleTleClient;
import com.realmone.tleasy.tle.SimpleTleFilter;
import com.realmone.tleasy.tle.TleUtils;

import javax.net.ssl.SSLException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class TLEasy extends JFrame {

    // Swing Components
    private final JTextField idField;
    private final JButton downloadButton;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JMenuBar menuBar;
    private final JMenu advMenu;
    private final JMenuItem configurationItem;

    // TLEasy Variables
    private static TleClient client;

    private static final JFileChooser fileChooser = new JFileChooser();

    static {
        fileChooser.setDialogTitle("Save TLE File");
    }

    public TLEasy() {
        // Frame setup
        setTitle("TLEasy");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        // Create Menu Bar
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        advMenu = new JMenu("Advanced");
        menuBar.add(advMenu);
        configurationItem = new JMenuItem("Configuration");
        advMenu.add(configurationItem);
        configurationItem.addActionListener(e -> configureAndSetupClient(false, false));

        // Create ID Field
        idField = new JTextField(20);

        // Create download button disabled by default
        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false);

        // Create progress bar for download
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        // Add a status label to the window
        statusLabel = new JLabel("Ready");
        statusLabel.setVisible(true);

        // Add components to frame
        add(new JLabel("Enter list of 5-digit ID(s) and/or range: "));
        add(idField);
        JLabel helpLabel = new JLabel("Separate with commas and hyphenate for a range.");
        helpLabel.setForeground(Color.GRAY);
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.ITALIC, 11f));
        add(helpLabel);
        add(downloadButton);
        add(progressBar);
        add(statusLabel);

        // Validation handler on ID Field to control disabling button
        idField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                checkID();
            }

            public void removeUpdate(DocumentEvent e) {
                checkID();
            }

            public void insertUpdate(DocumentEvent e) {
                checkID();
            }
        });

        // Button click handler for starting download
        downloadButton.addActionListener(e -> {
            progressBar.setVisible(true);
            downloadButton.setEnabled(false);
            statusLabel.setText("Downloading...");
            performDownload();
        });
    }

    /**
     * Builds the TleClient with the stored configuration.
     *
     * @throws IOException If something goes wrong setting up the SSL context
     */
    private static void setupClient() throws IOException {
        client = SimpleTleClient.builder()
                .tleDataEndpoint(Configuration.getTleDataEndpoint())
                .keystoreFile(Configuration.getKeyStoreFile())
                .keystorePassword(Configuration.getKeystorePassword())
                .skipCertValidation(Configuration.isSkipCertificateValidation())
                .build();
    }

    /**
     * Uses the {@link ConfigSetup} window to save the configuration and setup the {@link TleClient}. Loops until the
     * client can be initialized without exceptions.
     *
     * @param alreadyConfiguredCheck Whether to check if the configuration is set and not open the window if so
     */
    private static void configureAndSetupClient(boolean alreadyConfiguredCheck, boolean exitOnClose) {
        boolean exceptionThrown = false;
        do {
            try {
                if (Configuration.getKeyStoreFile() == null || !Configuration.getKeyStoreFile().exists()) {
                    JOptionPane.showMessageDialog(null, "Keystore could not be found.\nPlease update your configuration and save again.", "Error", JOptionPane.ERROR_MESSAGE);
                    new ConfigSetup(exitOnClose);
                } else if (exceptionThrown || !alreadyConfiguredCheck || !Configuration.isConfigured()) {
                    new ConfigSetup(exitOnClose);
                }
                setupClient();
                exceptionThrown = false;
            } catch (Exception e) {
                System.err.println("Issue setting up hooks for TLE Processing");
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to load configuration: " + e.getMessage() + "\nPlease re-enter your configuration and save again.", "Error", JOptionPane.ERROR_MESSAGE);
                exceptionThrown = true;
            }
        } while (exceptionThrown);
    }

    /**
     * Creates an error message dialog with the provided error message.
     *
     * @param message The error message to display
     */
    private void displayErrorMessage(String message) {
        progressBar.setVisible(false);
        downloadButton.setEnabled(true);
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Performs the download of the TLE data with the providing ID filters within a SwingWorker. Will display a file
     * chooser for the user to select the file name and download location for the filtered output. On completion, will
     * clear the text field, reset the download button, and remove the progress bar.
     */
    private void performDownload() {
        System.out.println("Performing download action");
        SwingWorker<Long, Void> worker = new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws Exception {
                System.out.println("Setting up filter for: " + idField.getText());
                TleFilter filter = SimpleTleFilter.builder()
                        .targetNoradIds(TleUtils.parseIdentifiers(idField.getText()))
                        .build();
                Optional<File> saveFile = getSaveFile();
                if (!saveFile.isPresent()) {
                    // TODO: figure out something better here
                    System.out.println("No save file selected");
                    return null;
                }
                try (InputStream data = client.fetchTle();
                     FileOutputStream output = new FileOutputStream(saveFile.get())) {
                    return filter.filter(data, output);
                } catch (IOException | InterruptedException ex) {
                    System.err.println("Exception thrown when pulling data: " + ex.getMessage());
                    ex.printStackTrace();
                    // If the exception is related to a trust issue, we want the user to make the decision to trust it
                    if (certificateTrustIssue(ex)) {
                        int result = JOptionPane.showConfirmDialog(null,
                                "Do you want to trust this server's certificate?",
                                "Trust Certificates", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if (result == JOptionPane.YES_OPTION) {
                            System.out.println("Trusting server certificates");
                            client.trustCerts();
                        } else {
                            throw ex;
                        }
                    } else {
                        throw ex;
                    }
                }
                // Trying again after trusting certs
                try (InputStream data = client.fetchTle();
                     FileOutputStream output = new FileOutputStream(saveFile.get())) {
                    return filter.filter(data, output);
                } catch (IOException | InterruptedException ex) {
                    System.err.println("Exception thrown when pulling data: " + ex.getMessage());
                    ex.printStackTrace();
                    throw ex;
                }
            }

            @Override
            protected void done() {
                try {
                    Long count = get();  // Retrieve the number of TLE entries downloaded
                    progressBar.setVisible(false);
                    downloadButton.setEnabled(false);
                    idField.setText("");

                    if (count != null) {
                        statusLabel.setText("Fetched " + count + " TLE entries");
                    } else {
                        statusLabel.setText("No TLE entries downloaded");
                    }

                } catch (Exception ex) {
                    displayErrorMessage("Download failed: " + ex.getMessage());
                    ex.printStackTrace();
                    statusLabel.setText("Download failed");
                }
            }
        };
        worker.execute();
    }

    /**
     * Creates a JFileChooser for the user to select the file name and location of the filtered output.
     *
     * @return An {@link Optional} of the File selected for the downloaded output; empty if the user cancelled the file
     * selection
     */
    private Optional<File> getSaveFile() {
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            return Optional.of(fileChooser.getSelectedFile());
        }
        return Optional.empty();
    }

    /**
     * Determines whether the ID field is a valid format and sets the enabled property on the download button
     * accordingly.
     */
    private void checkID() {
        String input = idField.getText();
        boolean isValid = validateInput(input);
        downloadButton.setEnabled(isValid);
    }

    /**
     * Returns a boolean representing whether the provided string is a valid ID input. Must be a single 5-digit ID, a
     * comma separate set of 5-digit IDs, or a hyphen separated range of 5-digit IDs.
     *
     * @param input A string value from the ID field
     * @return True if the value is valid; false otherwise
     */
    private boolean validateInput(String input) {
        if (input.isEmpty()) {
            return false;
        }
        // Split input by commas
        String[] parts = input.split("\\s*,\\s*");
        for (String part : parts) {
            if (part.matches("\\d{5}")) {
                continue;
            } else if (part.matches("\\d{5}\\s*-\\s*\\d{5}")) {
                String[] rangeParts = part.split("\\s*-\\s*");
                int start = Integer.parseInt(rangeParts[0]);
                int end = Integer.parseInt(rangeParts[1]);
                if (start > end) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine if an Exception message indicates that there is likely a trust issue with our internal keystore and
     * the remote certificate an endpoint presents.
     *
     * @param exception The Exception raised by the http connection being made
     * @return Whether we should allow the user to opportunity to trust the remote cert and try again
     */
    private static boolean certificateTrustIssue(Exception exception) {
        return exception instanceof SSLException &&
                (exception.getMessage().contains("trustAnchors parameter must be non-empty")
                        || exception.getMessage().contains("PKIX")
                        || exception.getMessage().contains("Self-signed or unknown CA"));
    }

    /**
     * Entry point for the application. Creates the TleClient and starts up the Swing application.
     *
     * @param args Arguments into the main method
     */
    public static void main(String... args) {
        SwingUtilities.invokeLater(() -> {
            configureAndSetupClient(true, true);
            TLEasy m = new TLEasy();
            m.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            m.setVisible(true);
        });
    }
}
