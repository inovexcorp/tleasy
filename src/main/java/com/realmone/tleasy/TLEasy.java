package com.realmone.tleasy;

import com.realmone.tleasy.rest.SimpleTleClient;
import com.realmone.tleasy.tle.SimpleTleFilter;
import com.realmone.tleasy.tle.TleUtils;

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
import java.net.URL;
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
import javax.swing.JFileChooser;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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

    // Default Colors
    private Color defaultBackground;
    private final Color darkBackground = Color.DARK_GRAY;
    private final Map<Component, Color[]> defaultColors = new HashMap<>();
    private final Map<Component, Color[]> darkColors = new HashMap<>();

    // TLEasy Variables
    private static TleClient client;

    // History/autocomplete
    private static final File HISTORY_FILE = new File(System.getProperty("user.home"), ".tleasy-history.txt");
    // TODO: Update this to the correct path from Dan, same as Configuration.java
    private final JWindow suggestionWindow = new JWindow();
    private final JList<String> suggestionList = new JList<>();
    private static final int HISTORY_LIMIT = 30;

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
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionWindow.setFocusableWindowState(false); // Keeps focus on text field
        JScrollPane suggestionScroll = new JScrollPane(suggestionList);
        suggestionWindow.add(suggestionScroll);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveHistoryToFile();
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

        // Create progress bar for download
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        // Add a status label to the window
        statusLabel = new JLabel("Ready");
        statusLabel.setVisible(true);

        // Enable hitting enter instead of clicking download, but not if autocomplete is open
        idField.addActionListener(e -> {
            if (!suggestionWindow.isVisible()) {
                downloadButton.doClick();
            }
        });

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
            statusLabel.setText("Downloading...");
            performDownload();
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
     */
    /**
     * Displays or hides a popup menu of autocomplete suggestions based on the current text.
     * The window is shown if the current input has matches in the history and hidden otherwise.
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

        // Window is only visible if we have suggestions
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
