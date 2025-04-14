package com.realmone.tleasy.ui;

import lombok.Builder;
import lombok.Singular;

import java.util.List;

/**
 * Simple validator to check that at least one of a list of {@link InputValidator}s is valid.
 */
@Builder
public class OrInputValidator implements InputValidator {

    @Singular
    private final List<InputValidator> validators;

    @Override
    public boolean isValid() {
        return validators.stream().anyMatch(InputValidator::isValid);
    }
}
