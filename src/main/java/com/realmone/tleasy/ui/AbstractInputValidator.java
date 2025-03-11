package com.realmone.tleasy.ui;

import javax.swing.JTextField;

public abstract class AbstractInputValidator implements InputValidator {
    protected final JTextField field;

    public AbstractInputValidator(JTextField field) {
        this.field = field;
    }
}
