package com.episim.model;

/**
 * Implemented by any domain object that can render itself as a line in a
 * plain-text report.
 */
public interface Reportable {

    /** @return the section title to print above this item's lines in a report */
    String getReportTitle();

    /** @return a single formatted line summarising this item */
    String toReportLine();
}
