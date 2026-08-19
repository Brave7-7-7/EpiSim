package com.episim.io;

import com.episim.model.DailyRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a CSV previously written by CsvExporter.exportDailyRecords() back into List<DailyRecord>.
 * Numbers are parsed with Integer.parseInt()/Double.parseDouble(), which are always locale-independent
 * — never a locale-sensitive java.text.NumberFormat — matching the files they read, which were written
 * with SimConstants.DATA_LOCALE regardless of this machine's default locale.
 */
public final class CsvImporter {

    private static final String EXPECTED_HEADER = "day,susceptible,exposed,infected,hospitalised,recovered,"
            + "deceased,new_infections,effective_r,beds_occupied,over_capacity";
    private static final int EXPECTED_COLUMN_COUNT = 11;

    private CsvImporter() {
    }

    /**
     * @param file a CSV file previously written by {@link CsvExporter#exportDailyRecords}
     * @return the parsed daily records, in file order
     * @throws IOException if the file cannot be read, its header does not match, or any row fails to
     *                      parse — the message names the exact file and line number
     */
    public static List<DailyRecord> importDailyRecords(Path file) throws IOException {
        List<DailyRecord> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            int lineNumber = 1;
            String header = reader.readLine();
            if (header == null || !header.trim().equals(EXPECTED_HEADER)) {
                throw new IOException("Unrecognised CSV header in " + file + " at line 1: expected \""
                        + EXPECTED_HEADER + "\" but found \"" + header + "\". "
                        + "This file was not produced by CsvExporter.exportDailyRecords().");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    records.add(parseRecord(line));
                } catch (RuntimeException e) {
                    throw new IOException("Failed to parse daily record CSV at line " + lineNumber
                            + " of " + file + ": \"" + line + "\" (" + e.getMessage() + ")", e);
                }
            }
        }
        return records;
    }

    /** Parses one CSV row into a {@link DailyRecord} (id/runId are not part of the CSV, so both are 0). */
    private static DailyRecord parseRecord(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length != EXPECTED_COLUMN_COUNT) {
            throw new IllegalArgumentException(
                    "expected " + EXPECTED_COLUMN_COUNT + " columns, found " + parts.length);
        }
        return new DailyRecord(0, 0,
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]),
                Integer.parseInt(parts[5]),
                Integer.parseInt(parts[6]),
                Integer.parseInt(parts[7]),
                Double.parseDouble(parts[8]),
                Integer.parseInt(parts[9]),
                Boolean.parseBoolean(parts[10]));
    }
}
