package com.episim;

import com.episim.dao.DataAccessException;
import com.episim.dao.DatabaseManager;
import com.episim.dao.PathogenDao;
import com.episim.engine.SimulationConfig;
import com.episim.engine.SimulationEngine;
import com.episim.gui.MainDashboard;
import com.episim.io.CsvExporter;
import com.episim.io.FileNameSanitizer;
import com.episim.model.ContactTracing;
import com.episim.model.Intervention;
import com.episim.model.Lockdown;
import com.episim.model.MaskMandate;
import com.episim.model.Pathogen;
import com.episim.model.VaccinationDrive;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Application entry point.
 *
 * <p>Normal launch ({@code java -jar episim.jar}) sets the Nimbus look and feel, boots the database,
 * and opens the GUI. Passing {@code --batch} instead runs three preset scenarios headlessly — writing
 * each to the database and exporting its daily-record CSV to {@code ./sample-output/} — then exits
 * without opening a window, for generating report data without a live demo.
 */
public class Main {

    /** @param args pass {@code --batch} to run headlessly instead of opening the GUI */
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--batch")) {
            runBatchMode();
            return;
        }
        runGuiMode();
    }

    private static void runGuiMode() {
        setNimbusLookAndFeel();

        if (!initialiseDatabaseOrShowError()) {
            return;
        }

        List<Pathogen> pathogens;
        try {
            pathogens = new PathogenDao().findAll();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load pathogens at startup", e);
        }
        if (pathogens.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "No pathogens are configured in the database. Delete data/episim.db and restart to reseed it.",
                    "No Pathogens Available", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new MainDashboard(pathogens).setVisible(true));
    }

    /**
     * Initialises the database, showing a friendly dialog — rather than letting a raw stack trace hit
     * the console with no visible window — if it cannot be opened (e.g. locked by another program, or
     * missing/unwritable directory) or fails its integrity check.
     *
     * @return {@code true} if the database is ready to use
     */
    private static boolean initialiseDatabaseOrShowError() {
        try {
            DatabaseManager.initialise();
        } catch (DataAccessException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Cannot Open Database", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!DatabaseManager.isHealthy()) {
            JOptionPane.showMessageDialog(null,
                    "The database at data/episim.db failed its integrity check. "
                            + "Delete the file and restart to rebuild it, or restore from a backup.",
                    "Database Integrity Check Failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private static void setNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception e) {
            // Nimbus is unavailable on this JVM; fall back to the platform default look and feel.
        }
    }

    // ---------- --batch mode ----------

    private static final int BATCH_POPULATION = 2000;
    private static final int BATCH_TOTAL_DAYS = 120;
    private static final int BATCH_SEED_INFECTIONS = 5;
    private static final long BATCH_RANDOM_SEED = 42L;
    private static final Path SAMPLE_OUTPUT_DIR = Path.of("sample-output");

    private static void runBatchMode() {
        // No display is ever touched in this mode, but setting this defensively means a CI machine with
        // no display attached still can't trip a HeadlessException if anything upstream tries.
        System.setProperty("java.awt.headless", "true");

        System.out.println("EpiSim batch mode: running three preset scenarios...");

        try {
            DatabaseManager.initialise();
        } catch (DataAccessException e) {
            System.err.println("Failed to open the database: " + e.getMessage());
            System.exit(1);
            return;
        }
        if (!DatabaseManager.isHealthy()) {
            System.err.println("Database failed its integrity check; delete data/episim.db and retry.");
            System.exit(1);
            return;
        }

        try {
            Files.createDirectories(SAMPLE_OUTPUT_DIR);
        } catch (IOException e) {
            System.err.println("Failed to create " + SAMPLE_OUTPUT_DIR.toAbsolutePath() + ": " + e.getMessage());
            System.exit(1);
            return;
        }

        Pathogen covid = findPathogenOrExit("COVID-19 (Delta-like)");

        runScenario("No Intervention", covid, List.of());
        runScenario("Masks Only", covid, List.of(buildMaskMandate()));
        runScenario("Full Response", covid,
                List.of(buildLockdown(), buildMaskMandate(), buildVaccinationDrive(), buildContactTracing()));

        System.out.println("Batch run complete. CSVs written to " + SAMPLE_OUTPUT_DIR.toAbsolutePath());
        System.exit(0);
    }

    private static Pathogen findPathogenOrExit(String name) {
        try {
            return new PathogenDao().findAll().stream()
                    .filter(p -> p.getName().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Pathogen '" + name + "' not found in the database"));
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Failed to load pathogen '" + name + "': " + e.getMessage());
            System.exit(1);
            throw new IllegalStateException(e); // unreachable; keeps the compiler happy about the return type
        }
    }

    private static void runScenario(String scenarioName, Pathogen pathogen, List<Intervention> interventions) {
        System.out.println("Running scenario: " + scenarioName);

        SimulationConfig config = new SimulationConfig()
                .setRunName(scenarioName)
                .setPathogen(pathogen)
                .setPopulationSize(BATCH_POPULATION)
                .setTotalDays(BATCH_TOTAL_DAYS)
                .setSeedInfections(BATCH_SEED_INFECTIONS)
                .setHealthcareWorkerRatio(0.05)
                .setElderlyRatio(0.12)
                .setRandomSeed(BATCH_RANDOM_SEED);

        SimulationEngine engine = new SimulationEngine(config);
        for (Intervention intervention : interventions) {
            engine.addIntervention(intervention);
        }
        engine.runAll();

        Path file = SAMPLE_OUTPUT_DIR.resolve(FileNameSanitizer.sanitize(scenarioName) + "_daily_records.csv");
        CsvExporter.exportDailyRecords(engine.getHistory(), file);
        System.out.println("  Run id " + engine.getRunId() + " completed (" + engine.getHistory().size()
                + " days) — wrote " + file.toAbsolutePath());
    }

    private static Intervention buildLockdown() {
        return new Lockdown(0, 0, "Lockdown", 10, 45, 0.8, 50_000.0, true);
    }

    private static Intervention buildMaskMandate() {
        return new MaskMandate(0, 0, "Mask Mandate", 14, BATCH_TOTAL_DAYS, 0.7, 5_000.0, true);
    }

    private static Intervention buildVaccinationDrive() {
        return new VaccinationDrive(0, 0, "Vaccination Drive", 20, BATCH_TOTAL_DAYS, 0.6, 20_000.0, true, 500);
    }

    private static Intervention buildContactTracing() {
        return new ContactTracing(0, 0, "Contact Tracing", 5, BATCH_TOTAL_DAYS, 0.6, 15_000.0, true, 300);
    }
}
