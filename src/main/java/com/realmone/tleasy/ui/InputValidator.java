package com.realmone.tleasy.ui;

/**
 * Simple API for validating that a {@link javax.swing.JTextField} is valid according to criteria based off the
 * implementation class. The constructor should take a JTextField as an argument and use that to provide the output
 * of the isValid interface.
 */
public interface InputValidator {

    boolean isValid();
}
