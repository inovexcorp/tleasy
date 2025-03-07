package com.realmone.tleasy.ui;

import lombok.Builder;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.regex.Pattern;

public class RegexDocumentListener implements DocumentListener {

    private final JTextField textFieldControl;
    private final JButton buttonControl;
    private final Pattern checkPattern;

    @Builder
    private RegexDocumentListener(JButton button, JTextField textField, String pattern) {
        buttonControl = button;
        textFieldControl = textField;
        checkPattern = Pattern.compile(pattern);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        processEvent();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        processEvent();
    }


    @Override
    public void changedUpdate(DocumentEvent e) {
        processEvent();
    }

    /**
     * Handle any event with the same logic, controlling the state of the configured {@link JButton}
     */
    private void processEvent() {
        // Control the state of the configured button
        buttonControl.setEnabled(
                // Only set the control to enabled if the text input is valid
                valid(textFieldControl.getText()));
    }

    /**
     * Is the supplied text valid/consistent with the configured pattern
     *
     * @param text The text data from the form
     * @return Whether or not the text matches the pattern
     */
    private boolean valid(String text) {
        boolean valid = false;
        if (text != null) {
            return checkPattern.matcher(text).matches();
        }
        return valid;
    }
}
