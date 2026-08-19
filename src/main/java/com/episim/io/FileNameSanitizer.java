package com.episim.io;

/** Turns an arbitrary string (e.g. a user-entered run name) into a safe filename component. */
public final class FileNameSanitizer {

    private static final String FALLBACK = "run";

    private FileNameSanitizer() {
    }

    /**
     * Collapses any run of characters outside [A-Za-z0-9._-] into a single underscore — never one
     * underscore per illegal character, so a multi-character illegal sequence (e.g. " — ", or a colon
     * from an embedded HH:mm:ss timestamp) can't produce repeated separators — then trims any leading
     * or trailing underscore/dot left over. A null or blank input (or one that sanitises to nothing,
     * e.g. a string of only illegal characters) falls back to "run" rather than producing an empty or
     * malformed filename.
     */
    public static String sanitize(String raw) {
        String value = (raw == null || raw.isBlank()) ? FALLBACK : raw.trim();
        String sanitized = value.replaceAll("[^A-Za-z0-9._-]+", "_");
        sanitized = sanitized.replaceAll("^[_.]+", "").replaceAll("[_.]+$", "");
        return sanitized.isEmpty() ? FALLBACK : sanitized;
    }
}
