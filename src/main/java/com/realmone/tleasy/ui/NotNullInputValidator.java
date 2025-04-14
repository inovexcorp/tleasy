package com.realmone.tleasy.ui;

import lombok.Builder;

import javax.swing.JTextField;

@Builder
public class NotNullInputValidator implements InputValidator {

    private final JTextField field;

    @Override
    public boolean isValid() {
        return !field.getText().trim().isEmpty();
    }
}
