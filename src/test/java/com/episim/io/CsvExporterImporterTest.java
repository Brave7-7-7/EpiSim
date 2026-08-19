package com.episim.io;

import com.episim.model.DailyRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvExporterImporterTest {

    @Test
    void exportedDailyRecordsRoundTripThroughImport(@TempDir Path tempDir) throws IOException {
        List<DailyRecord> original = List.of(
                new DailyRecord(0, 1, 1, 95, 3, 2, 0, 0, 0, 2, 0.0, 0, false),
                new DailyRecord(0, 1, 2, 90, 4, 5, 1, 0, 0, 3, 1.5, 1, false),
                new DailyRecord(0, 1, 3, 80, 5, 10, 3, 2, 0, 5, 2.0, 3, true));

        Path file = tempDir.resolve("daily_records.csv");
        CsvExporter.exportDailyRecords(original, file);

        List<DailyRecord> imported = CsvImporter.importDailyRecords(file);

        assertEquals(original.size(), imported.size());
        for (int i = 0; i < original.size(); i++) {
            DailyRecord expected = original.get(i);
            DailyRecord actual = imported.get(i);
            assertEquals(expected.getDayNumber(), actual.getDayNumber());
            assertEquals(expected.getSusceptible(), actual.getSusceptible());
            assertEquals(expected.getExposed(), actual.getExposed());
            assertEquals(expected.getInfected(), actual.getInfected());
            assertEquals(expected.getHospitalised(), actual.getHospitalised());
            assertEquals(expected.getRecovered(), actual.getRecovered());
            assertEquals(expected.getDeceased(), actual.getDeceased());
            assertEquals(expected.getNewInfections(), actual.getNewInfections());
            assertEquals(expected.getEffectiveR(), actual.getEffectiveR(), 1e-9);
            assertEquals(expected.getBedsOccupied(), actual.getBedsOccupied());
            assertEquals(expected.isOverCapacity(), actual.isOverCapacity());
        }
    }

    @Test
    void importRejectsAFileWithTheWrongHeader(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("wrong_header.csv");
        Files.writeString(file, "not,the,right,header\n1,2,3,4\n", StandardCharsets.UTF_8);

        IOException exception = assertThrows(IOException.class, () -> CsvImporter.importDailyRecords(file));
        assertTrue(exception.getMessage().contains("header"), "Error should mention the header: " + exception.getMessage());
    }

    @Test
    void importReportsTheOffendingLineNumberOnAParseFailure(@TempDir Path tempDir) throws IOException {
        List<DailyRecord> valid = List.of(new DailyRecord(0, 1, 1, 100, 0, 0, 0, 0, 0, 0, 0.0, 0, false));
        Path file = tempDir.resolve("corrupt.csv");
        CsvExporter.exportDailyRecords(valid, file);

        // Append a malformed third line (line 3: header is line 1, the one valid record is line 2).
        Files.writeString(file, "not,a,valid,csv,row\n", StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);

        IOException exception = assertThrows(IOException.class, () -> CsvImporter.importDailyRecords(file));
        assertTrue(exception.getMessage().contains("line 3"),
                "Error should report the offending line number: " + exception.getMessage());
    }
}
