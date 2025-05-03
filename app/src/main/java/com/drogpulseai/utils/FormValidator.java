package com.drogpulseai.utils;

import android.content.Context;
import android.widget.EditText;

/**
 * Helper class for form validation
 */
public class FormValidator {

    private final Context context;

    public FormValidator(Context context) {
        this.context = context;
    }

    /**
     * Validation rule for required fields
     */
    public ValidationRule required(String message) {
        return value -> {
            if (value == null || value.trim().isEmpty()) {
                return message;
            }
            return null;
        };
    }

    /**
     * Validation rule for integer fields
     */
    public ValidationRule integer(String message) {
        return value -> {
            if (value == null || value.trim().isEmpty()) {
                return null; // Empty is valid, handle with required if needed
            }

            try {
                Integer.parseInt(value.trim());
                return null;
            } catch (NumberFormatException e) {
                return message;
            }
        };
    }

    /**
     * Validation rule for decimal fields
     */
    public ValidationRule decimal(String message) {
        return value -> {
            if (value == null || value.trim().isEmpty()) {
                return null; // Empty is valid, handle with required if needed
            }

            try {
                Double.parseDouble(value.trim());
                return null;
            } catch (NumberFormatException e) {
                return message;
            }
        };
    }

    /**
     * Validation rule for email fields
     */
    public ValidationRule email(String message) {
        return value -> {
            if (value == null || value.trim().isEmpty()) {
                return null; // Empty is valid, handle with required if needed
            }

            String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
            if (value.matches(emailPattern)) {
                return null;
            } else {
                return message;
            }
        };
    }

    /**
     * Validate a form using a ValidationMap
     * @return true if all validations pass, false otherwise
     */
    public boolean validate(ValidationMap validationMap) {
        boolean isValid = true;

        for (ValidationMap.Entry entry : validationMap.getEntries()) {
            EditText field = entry.getField();
            String value = field.getText().toString();

            for (ValidationRule rule : entry.getRules()) {
                String errorMessage = rule.validate(value);

                if (errorMessage != null) {
                    field.setError(errorMessage);
                    isValid = false;
                    break;
                }
            }
        }

        return isValid;
    }

    /**
     * Interface for validation rules
     */
    public interface ValidationRule {
        /**
         * Validate a value
         * @param value The value to validate
         * @return Error message if validation fails, null if validation passes
         */
        String validate(String value);
    }
}