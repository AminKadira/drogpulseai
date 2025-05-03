package com.drogpulseai.utils;

import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to map fields to validation rules
 */
public class ValidationMap {
    private final List<Entry> entries = new ArrayList<>();

    public void add(EditText field, FormValidator.ValidationRule... rules) {
        entries.add(new Entry(field, rules));
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static class Entry {
        private final EditText field;
        private final FormValidator.ValidationRule[] rules;

        public Entry(EditText field, FormValidator.ValidationRule[] rules) {
            this.field = field;
            this.rules = rules;
        }

        public EditText getField() {
            return field;
        }

        public FormValidator.ValidationRule[] getRules() {
            return rules;
        }
    }
}