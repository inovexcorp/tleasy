package com.realmone.tleasy.ui;

import org.junit.Test;

import javax.swing.JTextField;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class RegexInputValidatorTest {

    @Test
    public void testValidInput() {
        // This test assumes that a valid input consists of only alphanumeric characters
        JTextField textField = new JTextField("abc123");
        String regex = "^[a-zA-Z0-9]+$";
        RegexInputValidator validator = RegexInputValidator.builder()
                .field(textField)
                .pattern(regex)
                .build();
        assertTrue("Expected valid input", validator.isValid());
    }

    @Test
    public void testInvalidInput() {
        // This test uses an input with a space, which should not match the regex
        JTextField textField = new JTextField("abc 123");
        String regex = "^[a-zA-Z0-9]+$";
        RegexInputValidator validator = RegexInputValidator.builder()
                .field(textField)
                .pattern(regex)
                .build();
        assertFalse("Expected invalid input", validator.isValid());
    }

    @Test
    public void testEmptyInput() {
        // This test assumes empty input is considered invalid
        JTextField textField = new JTextField("");
        String regex = "^[a-zA-Z0-9]+$";
        RegexInputValidator validator = RegexInputValidator.builder()
                .field(textField)
                .pattern(regex)
                .build();
        assertFalse("Expected empty input to be invalid", validator.isValid());
    }
}
