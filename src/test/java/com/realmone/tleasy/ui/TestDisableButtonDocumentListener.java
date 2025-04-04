package com.realmone.tleasy.ui;

import static org.junit.Assert.*;

import javax.swing.JButton;

import org.junit.Test;

public class TestDisableButtonDocumentListener {

    @Test
    public void testAllValidatorsValid() {
        // Create a dummy JButton.
        JButton button = new JButton();
        // Create a validator that always returns true.
        InputValidator alwaysValid = () -> true;

        // Create the listener with a single valid validator.
        DisableButtonDocumentListener listener = new DisableButtonDocumentListener(button, alwaysValid);

        // Simulate a document insert event.
        listener.insertUpdate(null);

        // Expect the button to be enabled since all validators return true.
        assertTrue("Button should be enabled when all validators are valid", button.isEnabled());
    }

    @Test
    public void testOneValidatorInvalid() {
        // Create a dummy JButton.
        JButton button = new JButton();
        // Create two validators: one valid and one invalid.
        InputValidator alwaysValid = () -> true;
        InputValidator alwaysInvalid = () -> false;

        // Create the listener with both validators.
        DisableButtonDocumentListener listener = new DisableButtonDocumentListener(button, alwaysValid, alwaysInvalid);

        // Simulate a document insert event.
        listener.insertUpdate(null);

        // Expect the button to be disabled since one validator returns false.
        assertFalse("Button should be disabled if any validator is invalid", button.isEnabled());
    }

    @Test
    public void testDifferentDocumentEvents() {
        // Create a dummy JButton.
        JButton button = new JButton();
        // Use a single validator that returns true.
        InputValidator alwaysValid = () -> true;

        // Create the listener.
        DisableButtonDocumentListener listener = new DisableButtonDocumentListener(button, alwaysValid);

        // Simulate different document events.
        listener.insertUpdate(null);
        assertTrue("Button should be enabled after insertUpdate", button.isEnabled());

        // For completeness, simulate removeUpdate and changedUpdate.
        listener.removeUpdate(null);
        assertTrue("Button should be enabled after removeUpdate", button.isEnabled());

        listener.changedUpdate(null);
        assertTrue("Button should be enabled after changedUpdate", button.isEnabled());
    }
}
