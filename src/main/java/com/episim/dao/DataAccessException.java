package com.episim.dao;

/**
 * Unchecked wrapper around {@link java.sql.SQLException} (and other
 * persistence failures) so callers are never forced to catch checked SQL
 * exceptions they cannot meaningfully recover from, while the original
 * cause and a human-readable message are always preserved — never swallowed.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
