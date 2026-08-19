package com.episim.io;

/** Unchecked wrapper around file I/O failures during CSV/text report export or import. */
public class ReportIoException extends RuntimeException {

    public ReportIoException(String message, Throwable cause) {
        super(message, cause);
    }
}
