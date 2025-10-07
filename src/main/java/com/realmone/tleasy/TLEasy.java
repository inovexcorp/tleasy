package com.realmone.tleasy;

import com.realmone.tleasy.rest.SimpleTleClient;
import com.realmone.tleasy.tle.SimpleTleFilter;
import com.realmone.tleasy.tle.TleUtils;
import com.realmone.tleasy.util.AccessReportGenerator;
import com.realmone.tleasy.util.StkCon;

import java.awt.Color;
import java.awt.Point;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.net.ssl.SSLException;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JWindow;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.Random;

public class TLEasy extends JFrame {

    // Swing Components
    private final JTextField idField;
    private final Set<String> inputHistory = new LinkedHashSet<>();
    private final JButton downloadButton;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JMenuBar menuBar;
    private final JMenu advMenu;
    private final JMenuItem configurationItem;
    private final JButton openInStkButton;

    // Default Colors
    private Color defaultBackground;
    private final Color darkBackground = Color.DARK_GRAY;
    private final Map<Component, Color[]> defaultColors = new HashMap<>();
    private final Map<Component, Color[]> darkColors = new HashMap<>();

    // TLEasy Variables
    private static TleClient client;

    // History/autocomplete
    private static final File HISTORY_FILE = new File(System.getProperty("user.home"), ".tleasy-history.txt");
    private final JWindow suggestionWindow = new JWindow();
    private final JList<String> suggestionList = new JList<>();
    private static final int HISTORY_LIMIT = 30;

    // STK
    private StkCon stkConnection;
    private static final String STK_EXECUTABLE_TO_USE = "AgUiApplication.exe";

    private static final JFileChooser fileChooser = new JFileChooser();

    static {
        fileChooser.setDialogTitle("Save TLE File");
    }

    private static final Random RANDOM = new Random();
    private static final String[] SILLY_MESSAGES = {
            "Reticulating splines...",
            "Brewing coffee...",
            "Dividing by zero...",
            "Herding cats..."
    };

    /**
     * Helper class to return multiple values from the SwingWorker
     */
    private static class StkWorkerResult {
        final String message;
        final String reportData;

        StkWorkerResult(String message, String reportData) {
            this.message = message;
            this.reportData = reportData;
        }
    }

    /**
     * Helper class to hold the results of TLE file generation.
     */
    private static class TleFileData {
        final File sanitizedFile;
        final List<String> satelliteNames;

        TleFileData(File sanitizedFile, List<String> satelliteNames) {
            this.sanitizedFile = sanitizedFile;
            this.satelliteNames = satelliteNames;
        }
    }

    public TLEasy() {
        // Frame setup
        setTitle("TLEasy");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionWindow.setFocusableWindowState(false); // Keeps focus on text field
        JScrollPane suggestionScroll = new JScrollPane(suggestionList);
        suggestionWindow.add(suggestionScroll);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveHistoryToFile();
                disconnectFromStk();
            }
        });

        // Load the icon from the resources folder
        try {
            URL resource = getClass().getResource("/tleasy.png");
            if (resource != null) {
                BufferedImage icon = ImageIO.read(resource);
                setIconImage(icon);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create Menu Bar
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        advMenu = new JMenu("Advanced");
        menuBar.add(advMenu);
        configurationItem = new JMenuItem("Configuration");
        advMenu.add(configurationItem);
        configurationItem.addActionListener(e -> {
            configureAndSetupClient(false, false);
            toggleDarkTheme(Configuration.isDarkTheme());
        });

        // Create ID Field and autocomplete
        loadHistoryFromFile();
        idField = new JTextField(20);
        idField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                // Delay preventing the window from closing before a click can be processed
                Timer timer = new Timer(150, (ae) -> {
                    if (!suggestionWindow.isFocusOwner() && !suggestionList.isFocusOwner() && !idField.isFocusOwner()) {
                        suggestionWindow.setVisible(false);
                    }
                });
                timer.setRepeats(false);
                timer.start();
            }
        });

        // Add arrow key navigation and enter selection to autocomplete dropdown
        setupIdFieldKeyListener();

        // Mouse listener for clicking the suggestion list
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selected = suggestionList.getSelectedValue();
                if (selected != null) {
                    acceptSuggestion(selected);
                    idField.requestFocusInWindow();
                }
            }
        });

        // Create download button disabled by default
        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false);

        // Create STK connection button disabled by default
        openInStkButton = new JButton("Open in STK");
        openInStkButton.setEnabled(false);

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
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(downloadButton);
        buttonPanel.add(openInStkButton);
        add(buttonPanel);
        add(progressBar);
        add(statusLabel);

        // Validation handler on ID Field to control disabling button and autocomplete
        idField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                handleInputChange();
            }
            public void removeUpdate(DocumentEvent e) {
                handleInputChange();
            }
            public void insertUpdate(DocumentEvent e) {
                handleInputChange();
            }
            private void handleInputChange() {
                checkID();
                showAutocompleteSuggestions();
            }
        });

        // Button click handler for starting download
        downloadButton.addActionListener(e -> {
            // ensures most recent entry will be at the top of the autocomplete list whether it's already there or not
            inputHistory.remove(idField.getText().trim());
            inputHistory.add(idField.getText().trim());
            progressBar.setVisible(true);
            downloadButton.setEnabled(false);
            setStatus("Downloading...");
            performDownload();
        });

        openInStkButton.addActionListener(e -> {
            inputHistory.remove(idField.getText().trim());
            inputHistory.add(idField.getText().trim());
            generateAndOpenInStk();
        });

        setColorMaps();
        toggleDarkTheme(Configuration.isDarkTheme());
    }

    /**
     * Populates the default and dark theme color maps for all components for use when toggling the theme. Uses an array
     * of {@link Color} objects to represent the foreground and background settings in that order.
     */
    private void setColorMaps() {
        this.defaultBackground = getContentPane().getBackground();
        defaultColors.put(menuBar, new Color[]{menuBar.getForeground(), menuBar.getBackground()});
        darkColors.put(menuBar, new Color[]{Color.BLACK, Color.GRAY});
        defaultColors.put(advMenu, new Color[]{advMenu.getForeground(), advMenu.getBackground()});
        darkColors.put(advMenu, new Color[]{Color.BLACK, Color.GRAY});
        defaultColors.put(configurationItem, new Color[]{configurationItem.getForeground(), configurationItem.getBackground()});
        darkColors.put(configurationItem, new Color[]{Color.BLACK, Color.GRAY});
        for (java.awt.Component comp : getContentPane().getComponents()) {
            if (comp instanceof JLabel) {
                defaultColors.put(comp, new Color[]{comp.getForeground()});
                darkColors.put(comp, new Color[]{Color.WHITE});
            } else if (comp instanceof JTextField) {
                defaultColors.put(comp, new Color[]{comp.getForeground(), comp.getBackground()});
                darkColors.put(comp, new Color[]{Color.WHITE, Color.GRAY});
            } else if (comp instanceof JButton || comp instanceof JProgressBar) {
                defaultColors.put(comp, new Color[]{comp.getForeground(), comp.getBackground()});
                darkColors.put(comp, new Color[]{Color.DARK_GRAY, Color.GRAY});
            }
        }
    }

    /**
     * Updates the styling of all components and the content pane to use dark theme or revert to default colors. Uses
     * the previously populated color maps to determine the appropriate colors.
     *
     * @param apply True if applying dark theme; false to use default colors
     */
    private void toggleDarkTheme(boolean apply) {
        // Set the background for the frame's content pane
        getContentPane().setBackground(apply ? this.darkBackground : this.defaultBackground);
        // Update opacity of menu bar and its items
        menuBar.setOpaque(apply);
        advMenu.setOpaque(apply);
        configurationItem.setOpaque(apply);
        // Identify Component Color Map to use
        Map<Component, Color[]> mapToUse = apply ? darkColors : defaultColors;
        // Update colors for manu bar and child components: labels, text fields, buttons, and progress bar
        Stream.concat(Arrays.stream(getContentPane().getComponents()), Stream.of(menuBar, advMenu, configurationItem))
                .forEach(comp -> {
                    if (mapToUse.containsKey(comp)) {
                        comp.setForeground(mapToUse.get(comp)[0]);
                        if (mapToUse.get(comp).length == 2) {
                            comp.setBackground(mapToUse.get(comp)[1]);
                        }
                    }
                });
    }

    /**
     * Builds the TleClient with the stored configuration.
     *
     * @throws IOException If something goes wrong setting up the SSL context
     */
    private static void setupClient() throws IOException {
        client = SimpleTleClient.builder()
                .tleFile(Configuration.getTleFile())
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
            private File saveFile;
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
                    // Limit of how big the autocomplete list can be is enforced here:
                    while (inputHistory.size() > HISTORY_LIMIT) {
                        Iterator<String> it = inputHistory.iterator();
                        if (it.hasNext()) {
                            it.next();
                            it.remove();
                        }
                    }

                    if (count == null) {
                        // This case handles when the user cancels the file save dialog.
                        setStatus("Download cancelled.");
                    } else if (count > 0) {
                        // This is the successful case.
                        setStatus("Fetched " + count + " TLE entries to file.");
                    } else { // This case handles when the filter runs but finds 0 entries.
                        displayErrorMessage("No TLE entries found for the specified IDs.");
                        setStatus("No TLEs found.");
                    }

                } catch (Exception ex) {
                    displayErrorMessage("Download failed: " + ex.getMessage());
                    ex.printStackTrace();
                    setStatus("Download failed");
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
     * Parses the text from the ID input field to extract valid ID numbers.
     *
     * <p>
     * This method supports two input formats:
     * <ul>
     *   <li>Single 5-digit identifiers (e.g., "12345").</li>
     *   <li>Ranges of 5-digit identifiers specified with a hyphen (e.g., "12345-12350").
     *       In this case, all numbers in the inclusive range are added to the result.</li>
     * </ul>
     * The input string may contain multiple IDs or ranges separated by commas. Whitespace around
     * commas or hyphens is ignored. If a range is specified where the start number is greater than
     * the end number, an {@code IllegalArgumentException} is thrown.
     * </p>
     *
     * @return a {@link Set} of ID Strings extracted from the input; returns an empty set if no valid IDs
     * are found.
     */
    private Set<String> getIds() {
        String input = idField.getText().trim();
        Set<String> result = new HashSet<>();
        // Split input by commas to allow mixed single IDs and ranges
        String[] parts = input.split("\\s*,\\s*");
        for (String part : parts) {
            // If the part is a 5 digit identifier
            if (part.matches("\\d{5}")) {
                result.add(part);
            }
            // Else if the part is a range specification
            else if (part.matches("\\d{5}\\s*-\\s*\\d{5}")) {
                // Grab the start and end part
                String[] rangeParts = part.split("\\s*-\\s*");
                int start = Integer.parseInt(rangeParts[0]);
                int end = Integer.parseInt(rangeParts[1]);
                // Simple validation to ensure the first number is smaller than the second...
                if (start > end) {
                    throw new IllegalArgumentException("Range must be in ascending order.");
                }
                // Add all the target numbers in the specified range
                result.addAll(getNumbersBetween(start, end).stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet()));
            }
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
     * Determines whether the ID field is a valid format and sets the enabled property on the download button
     * accordingly.
     */
    private void checkID() {
        String input = idField.getText();
        boolean isValid = validateInput(input);
        downloadButton.setEnabled(isValid);
        openInStkButton.setEnabled(isValid);
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
     * Helper method to update the text field with a selected suggestion.
     * It replaces the current segment of text being typed, rather than the entire field.
     *
     * @param selected The suggestion chosen by the user.
     */
    private void acceptSuggestion(String selected) {
        String text = idField.getText();
        int lastComma = text.lastIndexOf(',');
        int lastDash = text.lastIndexOf('-');
        int lastSeparator = Math.max(lastComma, lastDash);

        String prefix = (lastSeparator == -1) ? "" : text.substring(0, lastSeparator + 1);

        // Use invokeLater to prevent race conditions with the DocumentListener
        SwingUtilities.invokeLater(() -> {
            idField.setText(prefix + selected);
            suggestionWindow.setVisible(false);
        });
    }

    /**
     * Displays a popup menu of autocomplete suggestions based on the current text in the ID field.
     * Suggestions are filtered from the stored input history to include only those that begin with
     * the current input, case-insensitively. Matching entries are shown in a dropdown menu beneath
     * the text field, and selecting a suggestion will populate the field and hide the menu.
     *
     */
    private void showAutocompleteSuggestions() {
        String text = idField.getText();
        int lastComma = text.lastIndexOf(',');
        int lastDash = text.lastIndexOf('-');
        int lastSeparator = Math.max(lastComma, lastDash);

        String currentPart = (lastSeparator == -1) ? text : text.substring(lastSeparator + 1);
        String input = currentPart.toLowerCase().trim();

        // Hide if input is empty
        if (input.isEmpty()) {
            suggestionWindow.setVisible(false);
            return;
        }

        List<String> matches = inputHistory.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
        Collections.reverse(matches);

        // Hide if no matches
        if (matches.isEmpty()) {
            suggestionWindow.setVisible(false);
            return;
        }

        // Populate if there are matches
        suggestionList.setListData(matches.toArray(new String[0]));
        suggestionList.setVisibleRowCount(Math.min(matches.size(), 5)); // Limit height

        // Position
        Point fieldLocation = idField.getLocationOnScreen();
        suggestionWindow.setLocation(fieldLocation.x, fieldLocation.y + idField.getHeight());
        suggestionWindow.setSize(idField.getWidth(), suggestionList.getPreferredScrollableViewportSize().height + 6);

        // Window is only visible if there are suggestions
        if (!suggestionWindow.isVisible()) {
            suggestionWindow.setVisible(true);
        }
    }

    /**
     * Loads previously entered autocomplete history from a file into the {@code inputHistory} collection.
     * Each line in the file is trimmed and added as a distinct entry. If the history file does not exist,
     * the method returns silently. If an I/O error occurs while reading, an error message is printed to {@code System.err}.
     */
    private void loadHistoryFromFile() {
        if (!HISTORY_FILE.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                inputHistory.add(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Failed to load autocomplete history: " + e.getMessage());
        }
    }

    /**
     * Checks if the STK process is currently running on Windows.
     * @return true if the process is found, false otherwise.
     */
    private boolean isStkRunning() {
        try {
            Process process = Runtime.getRuntime().exec("tasklist /FI \"IMAGENAME eq " + STK_EXECUTABLE_TO_USE + "\"");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(STK_EXECUTABLE_TO_USE)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Ensures STK is running and establishes a connection, with a timeout.
     * This method will launch STK if it's not already running, then poll
     * for a successful TCP connection.
     *
     * @return A connected StkCon instance.
     * @throws IOException if STK executable is not found or if the process times out.
     * @throws InterruptedException if the thread is interrupted.
     */
    private StkCon launchAndConnectToStk() throws IOException, InterruptedException {
        // Ensure the STK process is running.
        if (!isStkRunning()) {
            setStatus("STK not running. Launching now...");

            String primaryPath = null;
            // First try to get the primary path from configuration
            File exeFile = Configuration.getExeFile();
            if (exeFile != null) {
                primaryPath = exeFile.getAbsolutePath();
            }

            String fallbackPath = "C:\\Program Files\\AGI\\STK 12\\bin\\" + STK_EXECUTABLE_TO_USE;
            String pathToUse = null;

            // 1. Try to find the executable at the primary path
            if (primaryPath != null && !primaryPath.isEmpty() && new File(primaryPath).exists()) {
                pathToUse = primaryPath;
            }
            // 2. If not found, try the fallback path
            else if (new File(fallbackPath).exists()) {
                pathToUse = fallbackPath;
            }

            // 3. If the executable was found in one of the paths, launch it
            if (pathToUse != null) {
                setStatus("Launching STK from: " + pathToUse);
                new ProcessBuilder(pathToUse, "/pers", "STK").start();
                // Give it a moment to create the process before we start polling.
                Thread.sleep(3000);
            } else {
                // 4. If neither path worked, throw a comprehensive error
                String primaryPathForError = (primaryPath != null) ? primaryPath : "[Not Configured]";
                throw new IOException(STK_EXECUTABLE_TO_USE + " not found. Checked primary path: " +
                        primaryPathForError + " and fallback path: " + fallbackPath);
            }
        }

        // Now, poll for a successful connection.
        int maxRetries = 15;  // 15 retries * 3 seconds = 45 seconds timeout
        int interval = 3000; // 3 seconds

        for (int i = 0; i < maxRetries; i++) {
            setStatus("Waiting for STK to initialize... Attempt " + (i + 1) + "/" + maxRetries);

            StkCon tempConnection = new StkCon();
            if (tempConnection.connect() == 0) {
                // Success! The TCP server is ready.
                setStatus("Connection successful!");
                return tempConnection;
            }

            // Wait before the next attempt.
            Thread.sleep(interval);
        }

        // If we exit the loop, it means we timed out.
        throw new IOException("Timed out after 45 seconds waiting for STK to become ready for connection.");
    }

    /**
     * Orchestrates the entire process of generating a TLE file, loading it into STK,
     * and generating an access report. This is the entry point for the SwingWorker.
     */
    private void generateAndOpenInStk() {
        setStatus("Starting STK process...");
        openInStkButton.setEnabled(false);
        downloadButton.setEnabled(false);

        SwingWorker<StkWorkerResult, Void> worker = new SwingWorker<StkWorkerResult, Void>() {
            @Override
            protected StkWorkerResult doInBackground() throws Exception {
                try {
                    // Generate and sanitize the TLE file
                    TleFileData tleData = generateSanitizedTleFile(idField.getText());

                    // Launch, Connect, and Load/Create Scenario
                    String scenarioName = establishStkConnectionAndScenario();

                    // Load all satellites into the scenario
                    loadAllSatellites(scenarioName, tleData);

                    // Create an instance of the new report generator class
                    AccessReportGenerator reportGenerator = new AccessReportGenerator(stkConnection, TLEasy.this);

                    // Generate the access report for all loaded satellites
                    String reportData;
                    if (Configuration.isCsv()) {
                        reportData = reportGenerator.generateAccessReportCsv(scenarioName, tleData.satelliteNames);
                    } else {
                        reportData = reportGenerator.generateAccessReportTxt(scenarioName, tleData.satelliteNames);
                    }

                    String message = (reportData == null || reportData.isEmpty())
                            ? "Loaded satellites, but no facility access found."
                            : "Access report generated successfully.";

                    return new StkWorkerResult(message, reportData);

                } catch (Exception e) {
                    // Ensure disconnection on any failure
                    if (stkConnection != null) stkConnection.disconnect();
                    throw e; // Re-throw to be caught by done()
                } finally {
                    // Always disconnect when the background task is finished
                    if (stkConnection != null) stkConnection.disconnect();
                }
            }

            @Override
            protected void done() {
                try {
                    StkWorkerResult result = get();
                    setStatus(result.message);
                    idField.setText("");

                    if (result.reportData != null && !result.reportData.isEmpty()) {
                        JFileChooser outputChooser = new JFileChooser();
                        outputChooser.setDialogTitle("Save Access Report file");
                        // Suggest a unique filename like "access_report (1).csv" if the original exists.
                        String preferredFilename;
                        if (Configuration.isCsv()) {
                            preferredFilename = "access_report.csv";
                        } else {
                            preferredFilename = "access_report.txt";
                        }
                        File uniqueFile = getUniqueFileForSaving(outputChooser, preferredFilename);
                        outputChooser.setSelectedFile(uniqueFile);
                        int userSelection = outputChooser.showSaveDialog(TLEasy.this);
                        if (userSelection == JFileChooser.APPROVE_OPTION) {
                            File fileToSave = outputChooser.getSelectedFile();
                            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                                writer.write(result.reportData);
                                setStatus(fileToSave.getName() + " saved");
                            } catch (IOException ex) {
                                displayErrorMessage("Failed to save report: " + ex.getMessage());
                            }
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    displayErrorMessage(cause.getMessage());
                    setStatus("Error: " + cause.getMessage());
                } finally {
                    checkID();
                }
            }
        };
        worker.execute();
    }

    /**
     * Generates a unique File object in the JFileChooser's current directory
     * to avoid overwriting existing files. If "basename.ext" exists, it will
     * try "basename (1).ext", "basename (2).ext", and so on.
     *
     * @param chooser The JFileChooser instance, used to get the target directory.
     * @param preferredFilename The desired initial filename (e.g., "access_report.csv").
     * @return A File object with a unique name in the target directory.
     */
    private File getUniqueFileForSaving(JFileChooser chooser, String preferredFilename) {
        File directory = chooser.getCurrentDirectory();
        File file = new File(directory, preferredFilename);

        // If the preferred name doesn't exist, use it right away.
        if (!file.exists()) {
            return file;
        }

        // Otherwise, separate the filename into its base and extension parts.
        String baseName = preferredFilename;
        String extension = "";
        int dotIndex = preferredFilename.lastIndexOf('.');

        // Ensure the dot is not the first character and an extension actually exists.
        if (dotIndex > 0 && dotIndex < preferredFilename.length() - 1) {
            baseName = preferredFilename.substring(0, dotIndex);
            extension = preferredFilename.substring(dotIndex + 1);
        }

        int counter = 1;
        // Loop, incrementing the counter, until we find a filename that doesn't exist.
        while (file.exists()) {
            String newFilename;
            if (extension.isEmpty()) {
                // Handle files with no extension.
                newFilename = String.format("%s (%d)", baseName, counter);
            } else {
                newFilename = String.format("%s (%d).%s", baseName, counter, extension);
            }
            file = new File(directory, newFilename);
            counter++;
        }
        return file;
    }

    /**
     * Filters and sanitizes TLE data from the source into a temporary file. This version
     * is robust and can handle both 2-line and 3-line TLE formats, generating default
     * names for satellites when the name line is missing.
     *
     * @param idFilter The user-provided filter string for NORAD IDs.
     * @return A TleFileData object containing the sanitized file and list of satellite names.
     * @throws Exception if no TLEs are found or file operations fail.
     */
    private TleFileData generateSanitizedTleFile(String idFilter) throws Exception {
        setStatus("Generating temporary TLE file...");
        TleFilter filter = SimpleTleFilter.builder()
                .targetNoradIds(TleUtils.parseIdentifiers(idFilter))
                .build();
        File originalTempTleFile = File.createTempFile("tleasy_original_", ".tle");
        originalTempTleFile.deleteOnExit();
        try (InputStream data = client.fetchTle();
             FileOutputStream output = new FileOutputStream(originalTempTleFile)) {
            long count = filter.filter(data, output);
            if (count == 0) {
                throw new Exception("No matching TLEs found for the given IDs.");
            }
        }

        File sanitizedTleFile = File.createTempFile("tleasy_sanitized_", ".tle");
        sanitizedTleFile.deleteOnExit();
        List<String> satelliteNames = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(originalTempTleFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(sanitizedTleFile))) {

            // Read all lines into a list to process them in chunks
            List<String> lines = reader.lines().collect(Collectors.toList());
            int i = 0;
            while (i < lines.size()) {
                String currentLine = lines.get(i).trim();

                // Skip any blank lines that might separate TLE chunks
                if (currentLine.isEmpty()) {
                    i++;
                    continue;
                }

                String nameLine;
                String tleLine1;
                String tleLine2;

                // Case 1: The current line is a TLE line 1 (2-line format or missing name)
                if (currentLine.startsWith("1 ")) {
                    tleLine1 = currentLine;

                    // The next line must be TLE line 2
                    if (i + 1 < lines.size() && lines.get(i + 1).trim().startsWith("2 ")) {
                        tleLine2 = lines.get(i + 1).trim();
                        // Extract ID from line 1 to generate a default name
                        String noradId = tleLine1.substring(2, 7).trim();
                        nameLine = "tle-" + noradId;
                        i += 2; // We have consumed two lines from the list
                    } else {
                        // Malformed entry: Found a Line 1 without a following Line 2. Skip it.
                        System.err.println("Warning: Found TLE Line 1 without a following Line 2. Skipping: " + tleLine1);
                        i++;
                        continue;
                    }
                }
                // Case 2: The current line is a name (standard 3-line format)
                else {
                    nameLine = currentLine;

                    // The next two lines must be TLE line 1 and line 2
                    if (i + 2 < lines.size() &&
                            lines.get(i + 1).trim().startsWith("1 ") &&
                            lines.get(i + 2).trim().startsWith("2 ")) {

                        tleLine1 = lines.get(i + 1).trim();
                        tleLine2 = lines.get(i + 2).trim();
                        i += 3; // We have consumed three lines from the list
                    } else {
                        // Malformed entry: A name line wasn't followed by TLE lines. Skip it.
                        System.err.println("Warning: Found a line that wasn't a TLE Line 1 and wasn't followed by a valid TLE set. Skipping: " + nameLine);
                        i++;
                        continue;
                    }
                }

                String sanitizedName = nameLine.trim().replace(" ", "_").replace("[", "").replace("]", "");
                satelliteNames.add(sanitizedName);

                writer.write(sanitizedName);
                writer.newLine();
                writer.write(tleLine1);
                writer.newLine();
                writer.write(tleLine2);
                writer.newLine();
            }
        }

        // Final check to ensure we actually processed something from the file
        if (satelliteNames.isEmpty()) {
            throw new Exception("The TLE data was present but could not be parsed into valid satellite entries.");
        }

        return new TleFileData(sanitizedTleFile, satelliteNames);
    }

    /**
     * Establishes a connection to STK and then loads or creates a scenario.
     * This version uses a robust connection method that polls until STK is fully ready.
     *
     * @return The name of the loaded or created scenario.
     * @throws Exception if a connection cannot be established or if scenario handling fails.
     */
    private String establishStkConnectionAndScenario() throws Exception {
        // First, try a quick connection in case STK is already running and ready.
        setStatus("Attempting to connect to STK...");
        stkConnection = new StkCon();

        // If the initial, simple connection attempt fails:
        if (stkConnection.connect() != 0) {
            // Start pollin'
            setStatus("STK not found or not ready. Starting connection process...");
            stkConnection = launchAndConnectToStk(); // This will return a valid connection or throw an exception.
        }

        // By this point, the connection is guaranteed to be established.
        setStatus("STK Connection Established.");

        String scenarioName;
        String scenarioCheck = stkConnection.sendConCommand("CheckScenario /");
        if (scenarioCheck.trim().equals("1")) {
            // A scenario is already open, use it!
            setStatus("Existing scenario detected. Using current scenario.");
            scenarioName = extractScenario(stkConnection.sendConCommand("AllInstanceNames / Scenario"));
        } else {
            // No scenario is open, so load or create one.
            File scenarioSaveFile = Configuration.getScenarioSaveFile();
            // Check if a save file path was configured.
            if (scenarioSaveFile != null) {
                String scenarioPath = scenarioSaveFile.getAbsolutePath();
                File scenarioFile = new File(scenarioPath);
                scenarioName = scenarioFile.getName().replace(".sc", "");

                if (scenarioFile.exists()) {
                    setStatus("Loading existing scenario: " + scenarioName);
                    stkConnection.sendConCommand("Load / Scenario \"" + scenarioPath + "\"");
                } else {
                    // If the file doesn't exist, create a new one with that name.
                    setStatus("No scenario found. Creating new one: " + scenarioName);
                    stkConnection.sendConCommand("New / Scenario " + scenarioName);
                }
            } else {
                // If no save file was configured, just create a brand new scenario with a default name.
                scenarioName = "TLEasy";
                setStatus("No save file configured. Creating new scenario: " + scenarioName);
                stkConnection.sendConCommand("New / Scenario " + scenarioName);
            }

            if (!stkConnection.getAckStatus()) {
                throw new Exception("Failed to load or create scenario.");
            }
        }
        return scenarioName;
    }

    /**
     * Get scenario name helper method
     */
    private String extractScenario(String input) {
        String[] parts = input.split("/Scenario/");
        // the last non-empty entry will always contain the scenario name
        String last = parts[parts.length - 1];
        String scenarioName = last.split("/")[0]; // take only the first part
        return scenarioName;
    }

    /**
     * Loads all satellite objects into the specified STK scenario.
     * @param scenarioName The name of the target scenario.
     * @param tleData The TLE data containing satellite names and the file path.
     * @throws Exception
     */
    private void loadAllSatellites(String scenarioName, TleFileData tleData) throws Exception {
        setStatus("Loading all satellites...");
        for (String satName : tleData.satelliteNames) {
            // Create the satellite object using the wildcard path
            stkConnection.sendConCommand("New / */Satellite " + satName);
            if (!stkConnection.getAckStatus()) {
                System.err.println("Warning: Failed to create satellite for " + satName);
                continue; // Skip to next satellite if creation fails
            }

            // Set the satellite's state from the TLE file
            String sscNumber = getSscNumberFromFile(tleData.sanitizedFile, satName);
            if (sscNumber.isEmpty()) {
                System.err.println("Warning: Could not find SSC number for " + satName);
                continue;
            }
            // Construct the explicit, full path for the SetState command
            String setStateFullPath = String.format("/Scenario/%s/Satellite/%s", scenarioName, satName);

            String setStateCommand = String.format(
                    "SetState %s SGP4 UseScenarioInterval 60.0 %s TLESource Automatic Source File \"%s\"",
                    setStateFullPath,
                    sscNumber,
                    tleData.sanitizedFile.getAbsolutePath()
            );
            stkConnection.sendConCommand(setStateCommand);
            if (!stkConnection.getAckStatus()) {
                System.err.println("Warning: Failed to set state for satellite " + satName);
            }
        }
        // Add a small pause after all satellites are loaded to ensure STK is fully caught up
        setStatus("Finalizing satellite propagation...");
        Thread.sleep(1000); // Wait 1 second
    }

    /**
     * A helper method to retrieve the SSC number for a satellite from a TLE file.
     * @param tleFile The file to search in.
     * @param satelliteName The name of the satellite.
     * @return The 5-digit SSC number as a String, or an empty string if not found.
     * @throws IOException
     */
    private String getSscNumberFromFile(File tleFile, String satelliteName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(tleFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals(satelliteName)) {
                    String tleLine1 = reader.readLine();
                    if (tleLine1 != null && tleLine1.length() >= 7) {
                        return tleLine1.substring(2, 7).trim();
                    }
                }
            }
        }
        return ""; // Not found
    }

    /**
     * Updates the status message. There is a 1 in 1000 chance
     * it will display a silly message instead.
     * @param message The intended status message.
     */
    public void setStatus(String message) {
        String finalMessage;

        if (RANDOM.nextInt(1000) == 0) {
            // Pick a random silly message from our array
            int index = RANDOM.nextInt(SILLY_MESSAGES.length);
            finalMessage = SILLY_MESSAGES[index];
        } else {
            // Otherwise, use the normal message
            finalMessage = message;
        }
        statusLabel.setText(finalMessage);
    }

    /**
     * Disconnects the socket connection to STK.
     */
    public void disconnectFromStk() {
        if (this.stkConnection != null) {
            this.stkConnection.disconnect();
            this.stkConnection = null;
        }
    }

    /**
     * Saves the current {@code inputHistory} entries to a file, one per line.
     * This method overwrites the existing history file with the current contents of {@code inputHistory}.
     * If an I/O error occurs while writing, an error message is printed to {@code System.err}.
     */
    private void saveHistoryToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HISTORY_FILE))) {
            for (String entry : inputHistory) {
                writer.write(entry);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save autocomplete history: " + e.getMessage());
        }
    }

    /**
     * Attaches a KeyListener to the idField component to enable
     * keyboard navigation and selection within the autocomplete suggestion list.
     * Supports directional arrows and enter.
     * This method is called during constructor.
     */
    private void setupIdFieldKeyListener() {
        idField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!suggestionWindow.isVisible()) return;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        int nextIndex = suggestionList.getSelectedIndex() + 1;
                        if (nextIndex < suggestionList.getModel().getSize()) {
                            suggestionList.setSelectedIndex(nextIndex);
                            suggestionList.ensureIndexIsVisible(nextIndex);
                        }
                        break;
                    case KeyEvent.VK_UP:
                        int prevIndex = suggestionList.getSelectedIndex() - 1;
                        if (prevIndex >= 0) {
                            suggestionList.setSelectedIndex(prevIndex);
                            suggestionList.ensureIndexIsVisible(prevIndex);
                        }
                        break;
                    case KeyEvent.VK_ENTER:
                        String selected = suggestionList.getSelectedValue();
                        if (selected != null) {
                            acceptSuggestion(selected);
                        }
                        e.consume(); // Prevent immediate form submission
                        break;
                    case KeyEvent.VK_ESCAPE:
                        suggestionWindow.setVisible(false);
                        break;
                }
            }
        });
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
     * @param args Arguments into the main method
     */
    public static void main(String... args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                configureAndSetupClient(true, true);
                TLEasy m = new TLEasy();
                m.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                m.setVisible(true);
            }
        });
    }
}