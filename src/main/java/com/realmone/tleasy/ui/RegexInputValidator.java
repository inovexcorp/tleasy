package com.realmone.tleasy.ui;

import lombok.Builder;

import java.util.regex.Pattern;
import javax.swing.JTextField;

public class RegexInputValidator implements InputValidator {

    private final JTextField field;

    private final Pattern checkPattern;

    @Builder
    private RegexInputValidator(JTextField field, String pattern) {
        this.field = field;
        this.checkPattern = Pattern.compile(pattern);
    }

    @Override
    public boolean isValid() {
        boolean valid = false; // Also false for null or empty
        String text = field.getText();
        if (text != null) {
            return checkPattern.matcher(text).matches();
        }
        return valid;
    }
}
