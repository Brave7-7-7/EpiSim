package com.episim.io;

/** Unchecked wrapper around file I/O failures during CSV/text report export or import. */
public class ReportIoException extends RuntimeException {

    /**
     * @param message human-readable description of what operation failed and why
     * @param cause   the underlying checked exception (typically an {@link java.io.IOException})
     */
    public ReportIoException(String message, Throwable cause) {
        super(message, cause);
    }
}
