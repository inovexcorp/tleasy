package com.realmone.tleasy.ui;

import lombok.Builder;
import lombok.Singular;

import java.util.List;

@Builder
public class OrInputValidator implements InputValidator {

    @Singular
    private final List<InputValidator> validators;

    @Override
    public boolean isValid() {
        return validators.stream().anyMatch(InputValidator::isValid);
    }
}
