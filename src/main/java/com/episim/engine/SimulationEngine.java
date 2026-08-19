package com.episim.engine;

import com.episim.dao.DailyRecordDao;
import com.episim.dao.DataAccessException;
import com.episim.dao.DatabaseManager;
import com.episim.dao.DistrictDao;
import com.episim.dao.EventLogDao;
import com.episim.dao.InterventionDao;
import com.episim.dao.PersonDao;
import com.episim.dao.SimulationRunDao;
import com.episim.model.DailyRecord;
import com.episim.model.District;
import com.episim.model.HealthState;
import com.episim.model.Intervention;
import com.episim.model.Pathogen;
import com.episim.model.Person;
import com.episim.model.SimulationRun;
import com.episim.util.SimConstants;

import javax.swing.Timer;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs a day-by-day SIR-style outbreak simulation over a generated
 * population, applying interventions, tracking hospital capacity, and
 * persisting progress to SQLite as it goes.
 */
public class SimulationEngine {

    private static final int FLUSH_INTERVAL_DAYS = 10;
    private static final double OVERWHELMED_MORTALITY_PENALTY = 1.8;
    private static final double RECOVERY_IMMUNITY_LEVEL = 0.9;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Assumption: districts are not hermetically sealed — commuting, shopping, family visits, etc. mean
    // a share of each person's daily contacts are drawn from the national population rather than
    // strictly their own district. Without this term, the four districts behave as four fully isolated
    // epidemics: with only a handful of seed infections, it's common for one or more districts to
    // receive zero seeds, and — since local force of infection is then permanently zero — that
    // district's residents can never be infected at all, no matter how high R0 is or how long the run
    // lasts. That caps the population-wide attack rate at the seeded districts' population share, well
    // below what final-size theory predicts for R0 above 1. 15% (within the 10-20% range commuting/
    // mixing studies typically suggest) restores a single, mostly-local-but-not-fully-isolated
    // transmission pool.
    private static final double CROSS_DISTRICT_MIXING_FRACTION = 0.15;

    private final SimulationConfig config;
    private final Pathogen pathogen;
    private final Random random;

    private final Map<String, District> districts = new HashMap<>();
    private final List<Person> population = new ArrayList<>();
    private final List<Intervention> interventions = new ArrayList<>();
    private final List<DailyRecord> history = new ArrayList<>();
    // Patients waiting for a hospital bed — demonstrates a Queue collection.
    private final Deque<Person> admissionQueue = new ArrayDeque<>();

    private final List<DailyRecord> pendingFlush = new ArrayList<>();
    private final List<SimulationListener> listeners = new ArrayList<>();

    private final DistrictDao districtDao = new DistrictDao();
    private final SimulationRunDao simulationRunDao = new SimulationRunDao();
    private final PersonDao personDao = new PersonDao();
    private final DailyRecordDao dailyRecordDao = new DailyRecordDao();
    private final InterventionDao interventionDao = new InterventionDao();
    private final EventLogDao eventLogDao = new EventLogDao();

    private int runId;
    private int currentDay;
    private int newInfectionsYesterday;
    private boolean started;
    private boolean finished;

    private volatile boolean paused;
    private Timer timer;

    public SimulationEngine(SimulationConfig config) {
        this.config = config;
        this.pathogen = config.getPathogen();
        this.random = new Random(config.getRandomSeed());
    }

    public void addListener(SimulationListener listener) {
        listeners.add(listener);
    }

    public void addIntervention(Intervention intervention) {
        interventions.add(intervention);
    }

    /** Loads districts, generates the population, and persists the run's opening state. */
    public void start() {
        if (started) {
            throw new IllegalStateException("Simulation has already been started");
        }
        loadDistricts();
        generatePopulation();
        persistRunStart();
        started = true;
    }

    /** Advances the simulation by exactly one day, finalising the run automatically once totalDays is reached. */
    public DailyRecord stepOneDay() {
        if (!started) {
            throw new IllegalStateException("Call start() before stepping the simulation");
        }
        if (finished) {
            throw new IllegalStateException("Simulation has already finished");
        }

        currentDay++;

        double transmissionModifier = 1.0;
        double severityModifier = 1.0;
        for (Intervention intervention : interventions) {
            if (intervention.isActiveOn(currentDay)) {
                // Runtime polymorphism: the correct subclass implementation of
                // transmissionModifier() is resolved at runtime.
                transmissionModifier *= intervention.transmissionModifier();
                severityModifier *= intervention.severityModifier();
            }
        }

        Map<String, Double> lambdaByDistrict = computeForceOfInfectionByDistrict(transmissionModifier);
        int newInfectionsToday = applyExposures(lambdaByDistrict);
        applyIncubationProgression();
        applyHospitalisation(severityModifier);
        applyResolution();
        advanceDaysInState();

        DailyRecord record = buildDailyRecord(newInfectionsToday);
        history.add(record);
        pendingFlush.add(record);
        newInfectionsYesterday = newInfectionsToday;

        for (SimulationListener listener : listeners) {
            listener.onDayCompleted(record);
        }

        if (currentDay % FLUSH_INTERVAL_DAYS == 0) {
            flushPendingRecords();
        }

        if (currentDay >= config.getTotalDays()) {
            finish("COMPLETED");
        }

        return record;
    }

    /** Runs the whole simulation synchronously, starting it first if needed. Used for headless/batch runs and tests. */
    public void runAll() {
        if (!started) {
            start();
        }
        while (!finished) {
            stepOneDay();
        }
    }

    /** Marks the run ABORTED (never deleted) and finalises persistence, if a run is in progress. */
    public void abort() {
        if (started && !finished) {
            finish("ABORTED");
        }
    }

    /** Stops any running timer, aborts an in-progress run, and clears all in-memory state for reuse. */
    public void reset() {
        stopTimer();
        abort();
        districts.clear();
        population.clear();
        interventions.clear();
        history.clear();
        admissionQueue.clear();
        pendingFlush.clear();
        currentDay = 0;
        newInfectionsYesterday = 0;
        runId = 0;
        started = false;
        finished = false;
        paused = false;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    /**
     * Drives the simulation from the GUI one tick at a time via a javax.swing.Timer, whose
     * ActionListener fires on the Event Dispatch Thread — this is what lets the GUI animate a run
     * without ever calling Thread.sleep() on the EDT and freezing the interface.
     */
    public void startTimer(int delayMillis) {
        if (!started) {
            start();
        }
        stopTimer();
        timer = new Timer(delayMillis, e -> {
            if (paused || finished) {
                return;
            }
            stepOneDay();
            if (finished) {
                stopTimer();
            }
        });
        timer.start();
    }

    public void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private void loadDistricts() {
        try {
            for (District district : districtDao.findAll()) {
                districts.put(district.getId(), district);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load districts for the simulation", e);
        }
        if (districts.isEmpty()) {
            throw new IllegalStateException("Cannot start a simulation with no districts in the database");
        }
        // district.hospital_capacity is sized against the design population (the sum of every
        // district's population column, e.g. 10,000 across the seed data) — not against
        // config.getPopulationSize(). Left unscaled, a small simulated population would have far more
        // beds per capita than intended, and the hospital-overwhelmed mechanic would become practically
        // unreachable. District.scaleHospitalCapacities() is shared with the GUI's "load historical run"
        // flow so both paths compute the same scaled capacity.
        District.scaleHospitalCapacities(districts.values(), config.getPopulationSize());
    }

    private void generatePopulation() {
        PopulationGenerator generator =
                new PopulationGenerator(new ArrayList<>(districts.values()), config.getRandomSeed());
        population.addAll(generator.generate(config));
        for (Person person : population) {
            districts.get(person.getDistrictId()).addResident(person);
        }
    }

    private void persistRunStart() {
        SimulationRun run = new SimulationRun(0, config.getRunName(), pathogen.getId(), config.getPopulationSize(),
                config.getTotalDays(), config.getSeedInfections(), config.getRandomSeed(), null, null,
                "RUNNING", null);
        try {
            simulationRunDao.insert(run);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert the simulation_run row at the start of the run", e);
        }
        runId = run.getId();
        personDao.insertBatch(population, runId);
    }

    private Map<String, Double> computeForceOfInfectionByDistrict(double transmissionModifier) {
        Map<String, List<Person>> byDistrict = population.stream()
                .collect(Collectors.groupingBy(Person::getDistrictId));

        long nationalInfectious = population.stream().filter(p -> p.getHealthState().isInfectious()).count();
        long nationalAlive = population.stream().filter(p -> p.getHealthState() != HealthState.DECEASED).count();
        double nationalInfectiousProportion = nationalAlive == 0 ? 0.0 : (double) nationalInfectious / nationalAlive;

        Map<String, Double> lambdaByDistrict = new HashMap<>();
        for (District district : districts.values()) {
            List<Person> residents = byDistrict.getOrDefault(district.getId(), List.of());
            long infectious = residents.stream().filter(p -> p.getHealthState().isInfectious()).count();
            long alive = residents.stream().filter(p -> p.getHealthState() != HealthState.DECEASED).count();
            double localInfectiousProportion = alive == 0 ? 0.0 : (double) infectious / alive;

            double infectiousProportion = (1 - CROSS_DISTRICT_MIXING_FRACTION) * localInfectiousProportion
                    + CROSS_DISTRICT_MIXING_FRACTION * nationalInfectiousProportion;

            // pathogen.perContactTransmissionProbability() is a PER-CONTACT probability (R0 divided by
            // infectiousDays * AVERAGE_DAILY_CONTACTS). To turn that back into a daily hazard we need:
            // per-contact probability x contacts per day x proportion of contacts who are infectious.
            // Without the AVERAGE_DAILY_CONTACTS factor here, lambda undershoots by exactly that factor
            // and R-effective collapses well below 1 even when R0 is well above it.
            double lambda = alive == 0 ? 0.0
                    : pathogen.perContactTransmissionProbability()
                            * SimConstants.AVERAGE_DAILY_CONTACTS
                            * infectiousProportion
                            * district.getDensityFactor()
                            * transmissionModifier;
            lambdaByDistrict.put(district.getId(), lambda);
        }
        return lambdaByDistrict;
    }

    private int applyExposures(Map<String, Double> lambdaByDistrict) {
        int newInfectionsToday = 0;
        for (Person person : population) {
            if (person.getHealthState() == HealthState.SUSCEPTIBLE) {
                double lambda = lambdaByDistrict.getOrDefault(person.getDistrictId(), 0.0);
                double hazard = lambda * person.getExposureMultiplier() * (1 - person.getImmunityLevel());
                // 1 - e^-hazard rather than a raw multiplication: a probability can never exceed 1, and
                // this form saturates towards 1 gracefully at high force of infection instead of needing
                // a separate clamp.
                double exposureProbability = 1 - Math.exp(-hazard);
                if (random.nextDouble() < exposureProbability) {
                    person.transitionTo(HealthState.EXPOSED);
                    newInfectionsToday++;
                }
            }
        }
        return newInfectionsToday;
    }

    private void applyIncubationProgression() {
        for (Person person : population) {
            if (person.getHealthState() == HealthState.EXPOSED
                    && person.getDaysInCurrentState() >= pathogen.getIncubationDays()) {
                person.transitionTo(HealthState.INFECTED);
            }
        }
    }

    private void applyHospitalisation(double severityModifier) {
        admitFromQueue();

        Set<String> loggedOverwhelmedDistrictsToday = new HashSet<>();
        for (Person person : population) {
            if (person.getHealthState() != HealthState.INFECTED) {
                continue;
            }
            double hospitalisationProbability =
                    pathogen.getHospitalisationRate() * person.getSeverityMultiplier() * severityModifier;
            if (random.nextDouble() >= hospitalisationProbability) {
                continue;
            }

            District district = districts.get(person.getDistrictId());
            if (district.occupiedBeds() < district.getHospitalCapacity()) {
                person.transitionTo(HealthState.HOSPITALISED);
            } else {
                // Healthcare system collapse: no bed is available, so this patient is queued instead of
                // admitted, and — per the resolution step — faces an elevated mortality penalty.
                admissionQueue.offer(person);
                if (loggedOverwhelmedDistrictsToday.add(district.getId())) {
                    eventLogDao.log(runId, currentDay, "HOSPITAL_OVERWHELMED",
                            "Hospital capacity exceeded in " + district.getName() + " ("
                                    + district.getHospitalCapacity() + " beds); patients are being queued for admission.");
                }
            }
        }
    }

    /** Admits patients already waiting on the queue if beds have freed up, oldest wait first (FIFO). */
    private void admitFromQueue() {
        int waitingCount = admissionQueue.size();
        for (int i = 0; i < waitingCount; i++) {
            Person waiting = admissionQueue.poll();
            if (waiting == null) {
                break;
            }
            if (waiting.getHealthState() != HealthState.INFECTED) {
                continue; // resolved (recovered/died) or otherwise no longer awaiting a bed
            }
            District district = districts.get(waiting.getDistrictId());
            if (district.occupiedBeds() < district.getHospitalCapacity()) {
                waiting.transitionTo(HealthState.HOSPITALISED);
            } else {
                admissionQueue.offer(waiting);
            }
        }
    }

    private void applyResolution() {
        for (Person person : population) {
            if (!person.getHealthState().isInfectious()
                    || person.getDaysInCurrentState() < pathogen.getInfectiousDays()) {
                continue;
            }
            District district = districts.get(person.getDistrictId());
            boolean overwhelmed = district.isHospitalOverwhelmed();
            double mortalityProbability = pathogen.getMortalityRate() * person.getSeverityMultiplier()
                    * (overwhelmed ? OVERWHELMED_MORTALITY_PENALTY : 1.0);
            if (random.nextDouble() < mortalityProbability) {
                person.transitionTo(HealthState.DECEASED);
            } else {
                person.transitionTo(HealthState.RECOVERED);
                person.setImmunityLevel(RECOVERY_IMMUNITY_LEVEL);
            }
        }
    }

    private void advanceDaysInState() {
        for (Person person : population) {
            person.setDaysInCurrentState(person.getDaysInCurrentState() + 1);
        }
    }

    private DailyRecord buildDailyRecord(int newInfectionsToday) {
        int susceptible = 0;
        int exposed = 0;
        int infected = 0;
        int hospitalised = 0;
        int recovered = 0;
        int deceased = 0;
        for (Person person : population) {
            switch (person.getHealthState()) {
                case SUSCEPTIBLE -> susceptible++;
                case EXPOSED -> exposed++;
                case INFECTED -> infected++;
                case HOSPITALISED -> hospitalised++;
                case RECOVERED -> recovered++;
                case DECEASED -> deceased++;
            }
        }

        int bedsOccupied = districts.values().stream().mapToInt(District::occupiedBeds).sum();
        boolean overCapacity = districts.values().stream().anyMatch(District::isHospitalOverwhelmed);
        double effectiveR = newInfectionsYesterday == 0 ? 0.0 : (double) newInfectionsToday / newInfectionsYesterday;

        return new DailyRecord(0, runId, currentDay, susceptible, exposed, infected, hospitalised, recovered,
                deceased, newInfectionsToday, effectiveR, bedsOccupied, overCapacity);
    }

    private void flushPendingRecords() {
        if (pendingFlush.isEmpty()) {
            return;
        }
        dailyRecordDao.insertBatch(new ArrayList<>(pendingFlush));
        pendingFlush.clear();
    }

    /**
     * Flushes remaining records, writes back final person health states, inserts the intervention rows,
     * and marks the run's status — all inside one transaction, so a crash never leaves the run's
     * persisted state half-updated.
     */
    private void finish(String status) {
        if (finished) {
            return;
        }
        finished = true;
        stopTimer();

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            try {
                dailyRecordDao.insertBatch(new ArrayList<>(pendingFlush), conn);
                pendingFlush.clear();

                personDao.updateHealthStates(population, conn);

                for (Intervention intervention : interventions) {
                    intervention.setRunId(runId);
                }
                interventionDao.insertBatch(interventions, conn);

                String completedAt = LocalDateTime.now().format(TIMESTAMP_FORMAT);
                simulationRunDao.updateStatus(runId, status, completedAt, conn);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new DataAccessException("Failed to finalise simulation run " + runId + " as " + status, e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to open the finalisation transaction for run " + runId, e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    throw new DataAccessException("Failed to restore autocommit or close the connection", e);
                }
            }
        }

        List<DailyRecord> finishedHistory = Collections.unmodifiableList(new ArrayList<>(history));
        for (SimulationListener listener : listeners) {
            listener.onSimulationFinished(finishedHistory);
        }
    }

    public SimulationConfig getConfig() {
        return config;
    }

    public List<Person> getPopulation() {
        return Collections.unmodifiableList(population);
    }

    public Map<String, District> getDistricts() {
        return Collections.unmodifiableMap(districts);
    }

    public List<DailyRecord> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public List<Intervention> getInterventions() {
        return Collections.unmodifiableList(interventions);
    }

    public int getAdmissionQueueSize() {
        return admissionQueue.size();
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public int getRunId() {
        return runId;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isPaused() {
        return paused;
    }
}
