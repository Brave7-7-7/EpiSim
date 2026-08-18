// TODO: replaced by MainDashboard in Prompt 3

package com.episim;

import com.episim.dao.DailyRecordDao;
import com.episim.dao.DatabaseManager;
import com.episim.dao.DistrictDao;
import com.episim.dao.PathogenDao;
import com.episim.dao.PersonDao;
import com.episim.dao.SimulationRunDao;
import com.episim.model.Citizen;
import com.episim.model.DailyRecord;
import com.episim.model.District;
import com.episim.model.ElderlyResident;
import com.episim.model.HealthState;
import com.episim.model.HealthcareWorker;
import com.episim.model.Pathogen;
import com.episim.model.Person;
import com.episim.model.RunSummary;
import com.episim.model.SimulationRun;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Database-only smoke test for the model/DAO layers built in Prompt 2.
 * No GUI — this is a throwaway diagnostic harness that will be replaced by
 * MainDashboard once the Swing screens exist.
 */
public class Main {

    private static boolean allPassed = true;

    public static void main(String[] args) throws SQLException {
        System.out.println("=== EpiSim database smoke test ===");
        // The console output below is display-only, so it deliberately keeps using the JVM's default
        // locale (unlike persisted/exported data, which always uses SimConstants.DATA_LOCALE). Printing
        // it here makes that difference visible — e.g. decimal points rendering as commas below.
        System.out.println("Default locale: " + Locale.getDefault());

        // --- 1. Initialise the database ---
        DatabaseManager.initialise();
        Path dbPath = Path.of("data/episim.db").toAbsolutePath();
        System.out.println("Database file: " + dbPath);
        check("Database file exists on disk", Files.exists(dbPath));

        // --- 2. Confirm seeding worked and did not duplicate ---
        PathogenDao pathogenDao = new PathogenDao();
        DistrictDao districtDao = new DistrictDao();

        List<Pathogen> pathogens = pathogenDao.findAll();
        List<District> districts = districtDao.findAll();

        System.out.println("\n-- Pathogens (" + pathogens.size() + ") --");
        for (Pathogen p : pathogens) {
            System.out.printf("  #%d %-24s R0=%.2f incubation=%dd infectious=%dd hosp=%.1f%% mortality=%.1f%%%n",
                    p.getId(), p.getName(), p.getR0(), p.getIncubationDays(), p.getInfectiousDays(),
                    p.getHospitalisationRate() * 100, p.getMortalityRate() * 100);
        }
        check("Pathogen table seeded exactly once (4 rows)", pathogens.size() == 4);

        System.out.println("\n-- Districts (" + districts.size() + ") --");
        for (District d : districts) {
            System.out.printf("  %-12s %-28s population=%-6d hospitalCapacity=%d%n",
                    d.getId(), d.getName(), d.getPopulation(), d.getHospitalCapacity());
        }
        check("District table seeded exactly once (4 rows)", districts.size() == 4);

        // --- 3. Insert a SimulationRun ---
        SimulationRunDao runDao = new SimulationRunDao();
        int pathogenId = pathogens.get(0).getId();

        SimulationRun run = new SimulationRun(0, "Smoke Test Run", pathogenId, 200, 30, 5, 42L,
                null, null, "RUNNING", "Created by the temporary database-only Main harness.");
        runDao.insert(run);
        System.out.println("\nInserted simulation_run with id=" + run.getId());
        check("Simulation run received a generated id", run.getId() > 0);

        // --- 4. Batch-insert 200 Person objects: a deliberate mix of the three subclasses ---
        List<Person> people = buildMixedPopulation(200, districts);

        PersonDao personDao = new PersonDao();
        long startNanos = System.nanoTime();
        personDao.insertBatch(people, run.getId());
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        System.out.println("\nBatch-inserted " + people.size() + " persons in " + elapsedMs + " ms");
        boolean allHaveIds = people.stream().allMatch(p -> p.getId() > 0);
        check("All 200 persons received generated ids", allHaveIds);

        // --- 5. Read back and verify polymorphic reconstruction ---
        List<Person> reloaded = personDao.findAll();

        Map<String, Long> byClass = reloaded.stream()
                .collect(Collectors.groupingBy(p -> p.getClass().getSimpleName(), Collectors.counting()));
        Map<String, Long> byRole = reloaded.stream()
                .collect(Collectors.groupingBy(Person::getRoleLabel, Collectors.counting()));

        System.out.println("\n-- Persons reloaded via PersonDao.findAll() (" + reloaded.size() + " total) --");
        System.out.println("By runtime class (getClass().getSimpleName()):");
        byClass.forEach((k, v) -> System.out.printf("  %-18s %d%n", k, v));
        System.out.println("By role label (getRoleLabel()):");
        byRole.forEach((k, v) -> System.out.printf("  %-18s %d%n", k, v));

        boolean allThreeSubclassesPresent = byClass.containsKey("Citizen")
                && byClass.containsKey("HealthcareWorker")
                && byClass.containsKey("ElderlyResident");
        check("All three Person subclasses reconstructed from person_type", allThreeSubclassesPresent);
        check("Class-name grouping and role-label grouping agree on total count",
                byClass.values().stream().mapToLong(Long::longValue).sum()
                        == byRole.values().stream().mapToLong(Long::longValue).sum());

        // --- 6. Insert 30 DailyRecord rows ---
        List<DailyRecord> dailyRecords = buildDailyRecords(run.getId(), 30, people.size());
        DailyRecordDao dailyRecordDao = new DailyRecordDao();
        dailyRecordDao.insertBatch(dailyRecords);
        boolean allDailyRecordsHaveIds = dailyRecords.stream().allMatch(r -> r.getId() > 0);
        System.out.println("\nBatch-inserted " + dailyRecords.size() + " daily_record rows");
        check("All 30 daily records received generated ids", allDailyRecordsHaveIds);

        // --- 7. Query v_run_summary ---
        List<RunSummary> summaries = runDao.findAllSummaries();
        RunSummary ourSummary = summaries.stream()
                .filter(s -> s.getRunId() == run.getId())
                .findFirst()
                .orElse(null);

        System.out.println("\n-- v_run_summary (run_id=" + run.getId() + ") --");
        if (ourSummary != null) {
            System.out.printf("  run='%s' pathogen='%s' population=%d totalDays=%d "
                            + "peakInfections=%d totalDeaths=%d totalInfections=%d daysOverCapacity=%d status=%s%n",
                    ourSummary.getRunName(), ourSummary.getPathogenName(), ourSummary.getPopulationSize(),
                    ourSummary.getTotalDays(), ourSummary.getPeakInfections(), ourSummary.getTotalDeaths(),
                    ourSummary.getTotalInfections(), ourSummary.getDaysOverCapacity(), ourSummary.getStatus());
        } else {
            System.out.println("  (no matching row found)");
        }
        check("v_run_summary contains a row for our run", ourSummary != null);

        // --- 8. Delete the run and confirm ON DELETE CASCADE fired ---
        runDao.delete(run.getId());
        int remainingPersons = countPersonsForRun(run.getId());
        System.out.println("\nDeleted simulation_run id=" + run.getId()
                + "; person rows remaining for that run: " + remainingPersons);
        check("PRAGMA foreign_keys=ON: deleting the run cascaded to its persons", remainingPersons == 0);

        // --- Summary ---
        System.out.println("\n=== " + (allPassed ? "ALL CHECKS PASSED" : "SOME CHECKS FAILED") + " ===");
        System.exit(allPassed ? 0 : 1);
    }

    private static List<Person> buildMixedPopulation(int count, List<District> districts) {
        Random random = new Random(42L);
        List<Person> people = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String districtId = districts.get(i % districts.size()).getId();
            boolean vaccinated = random.nextBoolean();
            double immunityLevel = random.nextDouble();

            switch (i % 3) {
                case 0 -> {
                    int age = 1 + random.nextInt(90);
                    people.add(new Citizen(0, "Citizen-" + i, age, districtId, HealthState.SUSCEPTIBLE,
                            0, vaccinated, immunityLevel));
                }
                case 1 -> {
                    int age = 22 + random.nextInt(40);
                    boolean hasPPE = random.nextBoolean();
                    String hospitalAssigned = "General Hospital " + (i % 3 + 1);
                    people.add(new HealthcareWorker(0, "HealthcareWorker-" + i, age, districtId,
                            HealthState.SUSCEPTIBLE, 0, vaccinated, immunityLevel, hasPPE, hospitalAssigned));
                }
                default -> {
                    int age = 60 + random.nextInt(35);
                    String careHomeName = "Care Home " + (i % 4 + 1);
                    people.add(new ElderlyResident(0, "ElderlyResident-" + i, age, districtId,
                            HealthState.SUSCEPTIBLE, 0, vaccinated, immunityLevel, careHomeName));
                }
            }
        }
        return people;
    }

    private static List<DailyRecord> buildDailyRecords(int runId, int totalDays, int population) {
        List<DailyRecord> records = new ArrayList<>(totalDays);
        int previousInfected = 0;

        for (int day = 1; day <= totalDays; day++) {
            int susceptible = Math.max(0, population - day * 6);
            int exposed = Math.max(0, 15 - day / 2);
            int infected = Math.max(0, (int) (30 * Math.exp(-0.1 * day)));
            int hospitalised = infected / 5;
            int recovered = Math.min(population, day * 5);
            int deceased = day / 10;
            int newInfections = Math.max(0, infected - previousInfected);
            double effectiveR = Math.max(0.1, 2.5 - day * 0.07);
            int bedsOccupied = hospitalised;
            boolean overCapacity = bedsOccupied > 50;

            records.add(new DailyRecord(0, runId, day, susceptible, exposed, infected, hospitalised,
                    recovered, deceased, newInfections, effectiveR, bedsOccupied, overCapacity));

            previousInfected = infected;
        }
        return records;
    }

    private static int countPersonsForRun(int runId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM person WHERE run_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static void check(String label, boolean condition) {
        System.out.println("[" + (condition ? "PASS" : "FAIL") + "] " + label);
        if (!condition) {
            allPassed = false;
        }
    }
}
