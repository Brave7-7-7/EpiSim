package com.episim.gui;

import com.episim.engine.OutbreakAnalyser;
import com.episim.io.CsvExporter;
import com.episim.io.CsvImporter;
import com.episim.io.FileNameSanitizer;
import com.episim.io.ReportIoException;
import com.episim.io.TextReportWriter;
import com.episim.model.DailyRecord;
import com.episim.model.District;
import com.episim.model.Intervention;
import com.episim.model.Person;
import com.episim.model.Reportable;
import com.episim.model.SimulationRun;
import com.episim.util.AppConfig;
import com.episim.util.SimConstants;
import com.episim.util.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** The Analysis tab: every {@link OutbreakAnalyser} metric, the narrative summary, and CSV/text export/import. */
public class AnalysisPanel extends JPanel {

    private final JLabel peakInfectionsValue = new JLabel("—");
    private final JLabel peakDayValue = new JLabel("—");
    private final JLabel peakBedsValue = new JLabel("—");
    private final JLabel attackRateValue = new JLabel("—");
    private final JLabel cfrValue = new JLabel("—");
    private final JLabel overCapacityValue = new JLabel("—");
    private final JLabel interventionCostValue = new JLabel("—");
    private final JTextArea narrativeArea = new JTextArea();

    private SimulationRun run;
    private List<DailyRecord> history = List.of();
    private List<District> districts = List.of();
    private List<Intervention> interventions = List.of();
    private List<Person> population = List.of();

    private Consumer<List<DailyRecord>> onCsvImported = imported -> {
    };

    /** Builds the stat grid, narrative area, and export/import button bar, initially showing "no data". */
    public AnalysisPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildStatGrid(), BorderLayout.NORTH);

        narrativeArea.setEditable(false);
        narrativeArea.setLineWrap(true);
        narrativeArea.setWrapStyleWord(true);
        narrativeArea.setFont(Theme.BODY_FONT);
        narrativeArea.setBackground(Theme.SURFACE);
        narrativeArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        add(new JScrollPane(narrativeArea), BorderLayout.CENTER);

        add(buildButtonBar(), BorderLayout.SOUTH);
        refreshStats();
    }

    private JComponent buildStatGrid() {
        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setBackground(Theme.BACKGROUND);
        grid.add(statCard("Peak Infections", peakInfectionsValue));
        grid.add(statCard("Peak Day", peakDayValue));
        grid.add(statCard("Peak Hospital Occupancy", peakBedsValue));
        grid.add(statCard("Attack Rate", attackRateValue));
        grid.add(statCard("Case Fatality Rate", cfrValue));
        grid.add(statCard("Days Over Capacity", overCapacityValue));
        grid.add(statCard("Total Intervention Cost", interventionCostValue));
        return grid;
    }

    private JComponent statCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.BODY_FONT);
        titleLabel.setForeground(Theme.TEXT_SECONDARY);
        valueLabel.setFont(Theme.TITLE_FONT.deriveFont(20f));
        valueLabel.setForeground(Theme.PRIMARY_DARK);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setBackground(Theme.BACKGROUND);

        JButton exportCsvButton = new JButton("Export CSV");
        exportCsvButton.addActionListener(e -> exportCsv());

        JButton exportTextButton = new JButton("Export Text Report");
        exportTextButton.addActionListener(e -> exportTextReport());

        JButton importCsvButton = new JButton("Import CSV");
        importCsvButton.addActionListener(e -> importCsv());

        bar.add(exportCsvButton);
        bar.add(exportTextButton);
        bar.add(importCsvButton);
        return bar;
    }

    /**
     * Registers a callback fired whenever "Import CSV" successfully loads a file — lets
     * {@code MainDashboard} update the Epidemic Curve tab with the imported data too.
     *
     * @param onCsvImported the callback to invoke with the newly imported daily records
     */
    public void setOnCsvImported(Consumer<List<DailyRecord>> onCsvImported) {
        this.onCsvImported = onCsvImported;
    }

    /**
     * Replaces the displayed dataset and refreshes every stat card and the narrative summary.
     *
     * @param run           the run being displayed, or {@code null} to show "no data"
     * @param history       the run's day-by-day history
     * @param districts     the run's districts, used for the text-report export
     * @param interventions the interventions deployed during the run
     * @param population    the run's population, used for the population CSV export and attack-rate calculation
     */
    public void setData(SimulationRun run, List<DailyRecord> history, List<District> districts,
                         List<Intervention> interventions, List<Person> population) {
        this.run = run;
        this.history = history;
        this.districts = districts;
        this.interventions = interventions;
        this.population = population;
        refreshStats();
    }

    private void refreshStats() {
        if (history.isEmpty()) {
            peakInfectionsValue.setText("—");
            peakDayValue.setText("—");
            peakBedsValue.setText("—");
            attackRateValue.setText("—");
            cfrValue.setText("—");
            overCapacityValue.setText("—");
            interventionCostValue.setText("—");
            narrativeArea.setText("No simulation data is available to summarise.");
            return;
        }

        int populationSize = population.size();
        peakInfectionsValue.setText(Integer.toString(OutbreakAnalyser.peakInfections(history)));
        peakDayValue.setText("Day " + OutbreakAnalyser.peakDay(history));
        peakBedsValue.setText(Integer.toString(OutbreakAnalyser.peakHospitalOccupancy(history)));
        attackRateValue.setText(String.format(SimConstants.DATA_LOCALE, "%.1f%%",
                OutbreakAnalyser.attackRate(history, populationSize) * 100));
        cfrValue.setText(String.format(SimConstants.DATA_LOCALE, "%.2f%%",
                OutbreakAnalyser.caseFatalityRate(history) * 100));
        overCapacityValue.setText(OutbreakAnalyser.daysHospitalOverCapacity(history) + " days");
        interventionCostValue.setText(String.format(SimConstants.DATA_LOCALE, "RM %.2f",
                OutbreakAnalyser.totalInterventionCost(interventions)));

        narrativeArea.setText(OutbreakAnalyser.generateNarrativeSummary(history, interventions, populationSize));
        narrativeArea.setCaretPosition(0);
    }

    private static final DateTimeFormatter FILENAME_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss", SimConstants.DATA_LOCALE);

    /**
     * A filesystem-safe base name: the run name (sanitised — colons, em-dashes, and anything else
     * outside [A-Za-z0-9._-] are collapsed to a single underscore, never one underscore per illegal
     * character, so a multi-character illegal run like " — " can't produce repeated separators) plus a
     * fresh export-time timestamp in the filename-safe yyyy-MM-dd_HHmmss form (never the display-oriented
     * HH:mm:ss embedded in the run name, which contains colons that are illegal in Windows filenames).
     * The fresh timestamp also means exporting the same run twice never silently overwrites the first.
     */
    private String baseFileName() {
        String runNamePart = FileNameSanitizer.sanitize(run != null ? run.getRunName() : null);
        String timestamp = LocalDateTime.now().format(FILENAME_TIMESTAMP);
        return runNamePart + "_" + timestamp;
    }

    /** Default export directory: AppConfig's export.directory (default "exports"), created if missing. */
    private Path resolveExportDirectory() {
        Path directory = Path.of(AppConfig.load().getExportDirectory());
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new ReportIoException("Failed to create export directory " + directory.toAbsolutePath(), e);
        }
        return directory;
    }

    private void rememberExportDirectory(Path directory) {
        AppConfig.load().setExportDirectory(directory.toString());
    }

    private void exportCsv() {
        if (run == null) {
            JOptionPane.showMessageDialog(this, "There is no run data to export yet.", "Export CSV",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Path defaultDirectory = resolveExportDirectory().toAbsolutePath();
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            // Point the chooser at the PARENT of the default directory and pre-select the directory
            // itself, rather than opening already positioned inside it. JFileChooser's
            // DIRECTORIES_ONLY + showSaveDialog() combination has a well-known quirk where clicking
            // Save with no explicit navigation resolves the selection to
            // <currentDirectory>/<currentDirectory's own name> — i.e. exports/exports.
            Path parent = defaultDirectory.getParent();
            chooser.setCurrentDirectory((parent != null ? parent : defaultDirectory).toFile());
            chooser.setSelectedFile(defaultDirectory.toFile());
            chooser.setDialogTitle("Choose a folder for the CSV export");
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path directory = chooser.getSelectedFile().toPath();
            Files.createDirectories(directory);
            Path absoluteDirectory = directory.toAbsolutePath();
            String base = baseFileName();

            CsvExporter.exportDailyRecords(history, directory.resolve(base + "_daily_records.csv"));
            CsvExporter.exportPopulation(population, directory.resolve(base + "_population.csv"));
            CsvExporter.exportRunSummary(run, interventions, directory.resolve(base + "_run_summary.csv"));

            System.out.println("Exported CSV files to: " + absoluteDirectory);
            rememberExportDirectory(directory);
            JOptionPane.showMessageDialog(this,
                    "Exported daily records, population, and run summary CSVs to:\n" + absoluteDirectory,
                    "Export CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            // Broad catch is deliberate: an export must never fail silently. java.nio.file's
            // InvalidPathException (e.g. from an illegal character surviving into a path) is an
            // unchecked IllegalArgumentException, not an IOException, so a narrower catch here would
            // let it escape uncaught — which is exactly what happened before this fix.
            JOptionPane.showMessageDialog(this, describeFailure(e), "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportTextReport() {
        if (run == null) {
            JOptionPane.showMessageDialog(this, "There is no run data to export yet.", "Export Text Report",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Path defaultDirectory = resolveExportDirectory();
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(defaultDirectory.toFile());
            chooser.setSelectedFile(new File(baseFileName() + "_report.txt"));
            chooser.setDialogTitle("Save text report");
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            Path file = chooser.getSelectedFile().toPath();
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);

            // Interface polymorphism, demonstrated by mixing two unrelated model classes behind one
            // Reportable list — see TextReportWriter.writeReport() for where toReportLine() is actually
            // called polymorphically.
            List<Reportable> items = new ArrayList<>();
            items.addAll(districts);
            items.addAll(history);
            String narrative = OutbreakAnalyser.generateNarrativeSummary(history, interventions, population.size());

            TextReportWriter.writeReport(file, run, items, narrative);

            Path absoluteFile = file.toAbsolutePath();
            System.out.println("Wrote text report to: " + absoluteFile);
            rememberExportDirectory(parent);
            JOptionPane.showMessageDialog(this, "Text report written to:\n" + absoluteFile,
                    "Export Text Report", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, describeFailure(e), "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importCsv() {
        try {
            Path defaultDirectory = resolveExportDirectory();
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(defaultDirectory.toFile());
            chooser.setDialogTitle("Import daily records CSV");
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            List<DailyRecord> imported = CsvImporter.importDailyRecords(chooser.getSelectedFile().toPath());
            this.history = imported;
            refreshStats();
            onCsvImported.accept(imported);
            System.out.println("Imported " + imported.size() + " daily records from: "
                    + chooser.getSelectedFile().getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Imported " + imported.size() + " daily records.",
                    "Import CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, describeFailure(e), "Import failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String describeFailure(Exception e) {
        String message = e.getMessage();
        return message != null && !message.isBlank()
                ? message
                : e.getClass().getSimpleName() + " (no further detail available)";
    }
}
