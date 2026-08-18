package com.episim.util;

import java.util.Locale;

/**
 * Project-wide constants shared by the model, engine, and persistence layers.
 */
public final class SimConstants {

    /** Average number of close contacts a person has per day, used to derive transmission probability. */
    public static final int AVERAGE_DAILY_CONTACTS = 12;

    // Locale policy: the lab machines this app runs on may have regional settings where the decimal
    // separator is a comma (e.g. "5,10" instead of "5.10"). GUI screens may format numbers with the
    // user's default locale for readability, but anything written to disk or the database — CSV/text
    // exports, report lines, and re-parsed numeric strings — must use DATA_LOCALE (Locale.ROOT) so the
    // format is stable across machines. When reading such data back, use Double.parseDouble(), which is
    // always locale-independent, never a locale-sensitive java.text.NumberFormat/DecimalFormat.
    public static final Locale DATA_LOCALE = Locale.ROOT;

    private SimConstants() {
    }
}
