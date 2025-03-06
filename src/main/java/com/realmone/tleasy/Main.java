package com.realmone.tleasy;

import com.realmone.tleasy.rest.SimpleTleClient;
import com.realmone.tleasy.tle.SimpleTleFilter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Main extends JFrame {

    // Swing Components
    private final JTextField idField;
    private final JButton downloadButton;
    private final JProgressBar progressBar;

    // TLEasy Variables
    private static TleClient client;

    public Main() {
        // TODO: Make prettier somehow
        // TODO: Add configuration form to update things
        // TODO: Add initial check for existing cert
        // Frame setup
        setTitle("TLEasy");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        // Create ID Field
        idField = new JTextField(20);

        // Create download button disabled by default
        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false);

        // Create progress bar for download
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        // Add components to frame
        // TODO: Add help text for valid inputs
        add(new JLabel("Enter 5-digit ID(s) or range: "));
        add(idField);
        add(downloadButton);
        add(progressBar);

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
            performDownload();
        });
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
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                TleFilter filter = SimpleTleFilter.builder()
                        .targetNoradIds(getIds())
                        .build();
                Optional<File> saveFile = getSaveFile();
                if (!saveFile.isPresent()) {
                    // TODO: figure out something better here
                    System.out.println("No save file selected");
                    return null;
                }
                try (InputStream data = client.fetchTle();
                     FileOutputStream output = new FileOutputStream(saveFile.get())) {
                    filter.filter(data, output);
                } catch (IOException | InterruptedException ex) {
                    System.err.println("Exception thrown when pulling data: " + ex.getMessage());
                    ex.printStackTrace();
                    throw ex;
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    progressBar.setVisible(false);
                    downloadButton.setEnabled(false);
                    idField.setText("");
                } catch (Exception ex) {
                    displayErrorMessage("Download failed: " + ex.getMessage());
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
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save TLE File");

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            return Optional.of(fileChooser.getSelectedFile());
        }
        return Optional.empty();
    }

    /**
     * Retrieves the Set of IDs specified in the ID field. If the ID field value does not match the expected format,
     * returns an empty Set.
     *
     * @return A {@link Set} of ID Strings to filter the TLE data by
     */
    private Set<String> getIds() {
        String input = idField.getText();
        if (isSingleId(input)) {
            return Collections.singleton(input);
        }

        if (isIdSet(input)) {
            return fromInput(input);
        }

        if (isIdRange(input)) {
            String[] range = input.split("\\s*-\\s*");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            return getNumbersBetween(start, end).stream().map(Object::toString).collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    private static Set<String> fromInput(String input) {
        Set<String> result = new HashSet<>();
        String[] parts = input.split(",");
        for (String part : parts) {
            result.add(part.trim());
        }
        return result;
    }

    /**
     * Creates a Set of integers representing the range of numbers between the provided start and end values.
     *
     * @param start The integer to start the range with
     * @param end   The integer to end the range with
     * @return A {@link Set} of {@link Integer}s representing all numbers from the start to the end inclusive
     */
    private Set<Integer> getNumbersBetween(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("Start number must be less than or equal to end number.");
        }

        Set<Integer> numbers = new HashSet<>();
        for (int i = start; i <= end; i++) {
            numbers.add(i);
        }
        return numbers;
    }

    /**
     * Checks whether the provided string input matches the format DDDDD where D is a digit.
     *
     * @param input The string to check
     * @return True if the string matches the expected format; false otherwise
     */
    private boolean isSingleId(String input) {
        return input.matches("\\d{5}");
    }

    /**
     * Checks whether the provided string input matches the format DDDDD,DDDDD,... where D is a digit.
     *
     * @param input The string to check
     * @return True if the string matches the expected format; false otherwise
     */
    private boolean isIdSet(String input) {
        return input.matches("\\d{5}(,\\s*\\d{5})*");
    }

    /**
     * Checks whether the provided string input matches the format DDDDD-DDDDD where D is a digit.
     *
     * @param input The string to check
     * @return True if the string matches the expected format; false otherwise
     */
    private boolean isIdRange(String input) {
        return input.matches("\\d{5}\\s*-\\s*\\d{5}");
    }


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

        // Single ID
        if (isSingleId(input)) {
            return true;
        }

        // Set of IDs
        if (isIdSet(input)) {
            return true;
        }

        // Range of IDs
        if (isIdRange(input)) {
            String[] range = input.split("\\s*-\\s*");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            return start <= end;
        }

        return false;
    }

    /**
     * Entry point for the application. Creates the TleClient and starts up the Swing application.
     *
     * @param args Arguments into the main method
     * @throws Exception if something goes wrong with the application startup
     */
    public static void main(String... args) throws Exception {
        if (!Configuration.isConfigured()) {
            Configuration.configure();
        }
        // TODO: Pull this from a config instead of command line
        client = SimpleTleClient.builder()
                .tleDataEndpoint(Configuration.getTleDataEndpoint())
                .keystoreFile(Configuration.getKeyStoreFile())
                .keystorePassword(Configuration.getKeystorePassword())
                .truststoreFile(Configuration.getTruststoreFile())
                .truststorePassword(Configuration.getTruststorePassword())
                .build();

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
