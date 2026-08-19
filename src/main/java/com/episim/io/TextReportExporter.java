package com.episim.io;

import com.episim.engine.OutbreakAnalyser;
import com.episim.model.DailyRecord;
import com.episim.model.District;
import com.episim.model.Intervention;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes a plain-text public-health report combining the narrative summary and per-day/per-district detail. */
public final class TextReportExporter {

    private TextReportExporter() {
    }

    public static void export(String runName, List<DailyRecord> history, List<District> districts,
                               List<Intervention> interventions, int populationSize, Path file) {
        StringBuilder report = new StringBuilder();
        report.append("EpiSim Outbreak Report — ").append(runName).append('\n');
        report.append("=".repeat(40)).append("\n\n");

        report.append(OutbreakAnalyser.generateNarrativeSummary(history, interventions, populationSize)).append("\n\n");

        report.append("Daily Records\n").append("-".repeat(40)).append('\n');
        for (DailyRecord record : history) {
            report.append(record.toReportLine()).append('\n');
        }

        report.append("\nDistrict Summary\n").append("-".repeat(40)).append('\n');
        for (District district : districts) {
            report.append(district.toReportLine()).append('\n');
        }

        try {
            Files.writeString(file, report.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ReportIoException("Failed to export text report to " + file, e);
        }
    }
}
