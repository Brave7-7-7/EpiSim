package com.episim.io;

import com.episim.model.DailyRecord;
import com.episim.util.SimConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports/imports a run's daily history as CSV. Numbers are written with SimConstants.DATA_LOCALE
 * (never the machine's default locale) and read back with Double.parseDouble(), which is always
 * locale-independent — see SimConstants for the full locale policy.
 */
public final class DailyRecordCsv {

    private static final String HEADER =
            "day,susceptible,exposed,infected,hospitalised,recovered,deceased,newInfections,effectiveR,bedsOccupied,overCapacity";

    private DailyRecordCsv() {
    }

    public static void export(List<DailyRecord> history, Path file) {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            writer.println(HEADER);
            for (DailyRecord r : history) {
                writer.println(String.format(SimConstants.DATA_LOCALE, "%d,%d,%d,%d,%d,%d,%d,%d,%.6f,%d,%b",
                        r.getDayNumber(), r.getSusceptible(), r.getExposed(), r.getInfected(), r.getHospitalised(),
                        r.getRecovered(), r.getDeceased(), r.getNewInfections(), r.getEffectiveR(),
                        r.getBedsOccupied(), r.isOverCapacity()));
            }
        } catch (IOException e) {
            throw new ReportIoException("Failed to export daily records to " + file, e);
        }
    }

    public static List<DailyRecord> importRecords(Path file) {
        List<DailyRecord> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // header, discarded
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",");
                records.add(new DailyRecord(0, 0,
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
                        Boolean.parseBoolean(parts[10])));
            }
        } catch (IOException | NumberFormatException e) {
            throw new ReportIoException("Failed to import daily records from " + file, e);
        }
        return records;
    }
}
