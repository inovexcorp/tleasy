package com.realmone.tleasy.ui;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class DisableButtonDocumentListener implements DocumentListener {

    private final JButton button;

    private final Set<InputValidator> validators;

    public DisableButtonDocumentListener(JButton button, InputValidator... validators) {
        this.button = button;
        this.validators = Arrays.stream(validators).collect(Collectors.toSet());
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        setButtonEnabled();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        setButtonEnabled();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        setButtonEnabled();
    }

    private void setButtonEnabled() {
        boolean allValid = validators.stream().allMatch(InputValidator::isValid);
        button.setEnabled(allValid);
    }
}
