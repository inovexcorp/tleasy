package com.realmone.tleasy.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OrInputValidatorTest {

    @Test
    public void testAllValidatorsValid() {
        // Create validators that always return true
        InputValidator validator1 = () -> true;
        InputValidator validator2 = () -> true;

        // Create OrInputValidator
        OrInputValidator orInputValidator = OrInputValidator.builder()
                .validator(validator1)
                .validator(validator2)
                .build();

        assertTrue(orInputValidator.isValid());
    }

    @Test
    public void testOneValidatorsValid() {
        // Create validators, one that returns true and one that returns false
        InputValidator validator1 = () -> true;
        InputValidator validator2 = () -> false;

        // Create OrInputValidator
        OrInputValidator orInputValidator = OrInputValidator.builder()
                .validator(validator1)
                .validator(validator2)
                .build();

        assertTrue(orInputValidator.isValid());
    }

    @Test
    public void testNoValidatorsValid() {
        // Create validators that always return false
        InputValidator validator1 = () -> false;
        InputValidator validator2 = () -> false;

        // Create OrInputValidator
        OrInputValidator orInputValidator = OrInputValidator.builder()
                .validator(validator1)
                .validator(validator2)
                .build();

        assertFalse(orInputValidator.isValid());
    }
}
