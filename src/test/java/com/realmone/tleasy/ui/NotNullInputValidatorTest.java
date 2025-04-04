package com.realmone.tleasy.ui;

import org.junit.Test;
import static org.junit.Assert.*;

import javax.swing.JTextField;

public class NotNullInputValidatorTest {

    @Test
    public void testIsValidNonEmpty() {
        JTextField textField = new JTextField("Hello");
        NotNullInputValidator validator = NotNullInputValidator.builder()
                .field(textField)
                .build();
        assertTrue("Validator should return true for non-empty text", validator.isValid());
    }

    @Test
    public void testIsValidEmpty() {
        JTextField textField = new JTextField("");
        NotNullInputValidator validator = NotNullInputValidator.builder()
                .field(textField)
                .build();
        assertFalse("Validator should return false for empty text", validator.isValid());
    }

    @Test
    public void testIsValidWhitespace() {
        JTextField textField = new JTextField("   ");
        NotNullInputValidator validator = NotNullInputValidator.builder()
                .field(textField)
                .build();
        assertFalse("Validator should return false for whitespace-only text", validator.isValid());
    }
}