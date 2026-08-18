package com.episim.model;

/**
 * Implemented by any domain object that can render itself as a line in a
 * plain-text report.
 */
public interface Reportable {

    String getReportTitle();

    String toReportLine();
}
