package com.episim.io;

import com.episim.model.Reportable;
import com.episim.model.SimulationRun;
import com.episim.util.SimConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Writes a fixed-width plain-text report combining run metadata, a narrative summary, and per-item detail. */
public final class TextReportWriter {

    private static final int WIDTH = 78;
    private static final int LABEL_WIDTH = 20;
    private static final int VALUE_WIDTH = 15;
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", SimConstants.DATA_LOCALE);

    private TextReportWriter() {
    }

    public static void writeReport(Path file, SimulationRun run, List<Reportable> items, String narrative) {
        StringBuilder report = new StringBuilder();

        report.append(centered("EPISIM OUTBREAK REPORT")).append('\n');
        report.append("=".repeat(WIDTH)).append('\n');
        report.append(labeledLine("Run:", run.getRunName()));
        report.append(labeledLine("Population:", run.getPopulationSize()));
        report.append(labeledLine("Total days:", run.getTotalDays()));
        report.append(labeledLine("Status:", run.getStatus()));
        report.append(labeledLine("Generated:", LocalDateTime.now().format(TIMESTAMP)));
        report.append("-".repeat(WIDTH)).append('\n').append('\n');

        report.append("SUMMARY\n");
        report.append("-".repeat(WIDTH)).append('\n');
        report.append(wrap(narrative, WIDTH)).append("\n\n");

        report.append("DETAIL\n");
        report.append("-".repeat(WIDTH)).append('\n');
        // Interface polymorphism: `items` mixes District and DailyRecord objects — two unrelated
        // branches of the model, sharing nothing but Reportable — behind a single List<Reportable>.
        // Each call below resolves to whichever concrete class's toReportLine() this element actually
        // is at runtime; this method never needs an instanceof check to know which.
        for (Reportable item : items) {
            report.append(item.toReportLine()).append('\n');
        }
        report.append("=".repeat(WIDTH)).append('\n');

        try {
            Files.writeString(file, report.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ReportIoException("Failed to write text report to " + file, e);
        }
    }

    private static String labeledLine(String label, String value) {
        return String.format(SimConstants.DATA_LOCALE, "%-" + LABEL_WIDTH + "s%" + VALUE_WIDTH + "s%n", label, value);
    }

    private static String labeledLine(String label, int value) {
        return String.format(SimConstants.DATA_LOCALE, "%-" + LABEL_WIDTH + "s%" + VALUE_WIDTH + "d%n", label, value);
    }

    private static String centered(String text) {
        int padding = Math.max(0, (WIDTH - text.length()) / 2);
        return " ".repeat(padding) + text;
    }

    private static String wrap(String text, int width) {
        StringBuilder wrapped = new StringBuilder();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                wrapped.append(line).append('\n');
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            wrapped.append(line);
        }
        return wrapped.toString();
    }
}
