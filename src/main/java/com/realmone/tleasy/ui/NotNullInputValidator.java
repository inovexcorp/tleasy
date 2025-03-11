package com.realmone.tleasy.ui;

import lombok.Builder;

import javax.swing.JTextField;

public class NotNullInputValidator implements InputValidator {

    private final JTextField field;

    @Builder
    private NotNullInputValidator(JTextField field) {
        this.field = field;
    }
    @Override
    public boolean isValid() {
        return !field.getText().trim().isEmpty();
    }
}
