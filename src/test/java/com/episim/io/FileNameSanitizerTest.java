package com.episim.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FileNameSanitizerTest {

    @Test
    void replacesColonsFromAnEmbeddedTimestamp() {
        // The exact reported bug: a run name containing "HH:mm:ss" broke Windows saves.
        assertEquals("Measles_23_15_42", FileNameSanitizer.sanitize("Measles 23:15:42"));
    }

    @Test
    void collapsesAMultiCharacterIllegalRunToOneUnderscoreNotOnePerCharacter() {
        // " — " is three characters (space, em-dash, space); the reported bug produced a *tripled*
        // underscore because each illegal character was being replaced individually.
        assertEquals("Measles_2026", FileNameSanitizer.sanitize("Measles — 2026"));
    }

    @Test
    void blankOrNullInputFallsBackToARun() {
        assertEquals("run", FileNameSanitizer.sanitize(""));
        assertEquals("run", FileNameSanitizer.sanitize("   "));
        assertEquals("run", FileNameSanitizer.sanitize(null));
    }

    @Test
    void aNameOfOnlyIllegalCharactersFallsBackToARunRatherThanAnEmptyString() {
        assertEquals("run", FileNameSanitizer.sanitize(":::///???"));
    }

    @Test
    void trimsLeadingAndTrailingSeparatorsLeftBySanitization() {
        assertEquals("Measles", FileNameSanitizer.sanitize(":Measles:"));
    }

    @Test
    void neverProducesRepeatedSeparatorsRegardlessOfInput() {
        String sanitized = FileNameSanitizer.sanitize("A///:::B");
        assertFalse(sanitized.contains("__"), "Should never contain repeated underscores: " + sanitized);
    }

    @Test
    void leavesAnAlreadySafeNameUnchanged() {
        assertEquals("COVID-19_Run.1", FileNameSanitizer.sanitize("COVID-19_Run.1"));
    }
}
