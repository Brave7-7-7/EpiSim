-- ============================================================
-- EpiSim — SQLite schema
-- Place at: src/main/resources/schema.sql
-- Executed by com.episim.dao.DatabaseManager.initialise()
-- Safe to run repeatedly (all statements use IF NOT EXISTS)
-- ============================================================

PRAGMA foreign_keys = ON;

-- ---------- Reference data ----------

CREATE TABLE IF NOT EXISTS pathogen (
    pathogen_id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name                  TEXT    NOT NULL UNIQUE,
    r0                    REAL    NOT NULL CHECK (r0 > 0),
    incubation_days       INTEGER NOT NULL CHECK (incubation_days >= 0),
    infectious_days       INTEGER NOT NULL CHECK (infectious_days > 0),
    hospitalisation_rate  REAL    NOT NULL CHECK (hospitalisation_rate BETWEEN 0 AND 1),
    mortality_rate        REAL    NOT NULL CHECK (mortality_rate BETWEEN 0 AND 1),
    vaccine_effectiveness REAL    NOT NULL DEFAULT 0.0 CHECK (vaccine_effectiveness BETWEEN 0 AND 1),
    description           TEXT
);

CREATE TABLE IF NOT EXISTS district (
    district_id      TEXT    PRIMARY KEY,
    name             TEXT    NOT NULL,
    population       INTEGER NOT NULL CHECK (population > 0),
    density_factor   REAL    NOT NULL DEFAULT 1.0 CHECK (density_factor > 0),
    hospital_capacity INTEGER NOT NULL CHECK (hospital_capacity >= 0)
);

-- ---------- Simulation runs ----------

CREATE TABLE IF NOT EXISTS simulation_run (
    run_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    run_name        TEXT    NOT NULL,
    pathogen_id     INTEGER NOT NULL,
    population_size INTEGER NOT NULL,
    total_days      INTEGER NOT NULL,
    seed_infections INTEGER NOT NULL,
    random_seed     INTEGER NOT NULL,
    started_at      TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    completed_at    TEXT,
    status          TEXT    NOT NULL DEFAULT 'RUNNING'
                            CHECK (status IN ('RUNNING','COMPLETED','ABORTED')),
    notes           TEXT,
    FOREIGN KEY (pathogen_id) REFERENCES pathogen(pathogen_id)
);

CREATE TABLE IF NOT EXISTS person (
    person_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id           INTEGER,
    full_name        TEXT    NOT NULL,
    age              INTEGER NOT NULL CHECK (age BETWEEN 0 AND 120),
    person_type      TEXT    NOT NULL
                             CHECK (person_type IN ('CITIZEN','HEALTHCARE_WORKER','ELDERLY')),
    district_id      TEXT    NOT NULL,
    health_state     TEXT    NOT NULL DEFAULT 'SUSCEPTIBLE'
                             CHECK (health_state IN ('SUSCEPTIBLE','EXPOSED','INFECTED',
                                                     'HOSPITALISED','RECOVERED','DECEASED')),
    days_in_state    INTEGER NOT NULL DEFAULT 0,
    vaccinated       INTEGER NOT NULL DEFAULT 0 CHECK (vaccinated IN (0,1)),
    immunity_level   REAL    NOT NULL DEFAULT 0.0 CHECK (immunity_level BETWEEN 0 AND 1),
    -- subclass-specific columns (NULL when not applicable)
    has_ppe          INTEGER CHECK (has_ppe IN (0,1)),
    hospital_assigned TEXT,
    care_home_name   TEXT,
    FOREIGN KEY (run_id)      REFERENCES simulation_run(run_id) ON DELETE CASCADE,
    FOREIGN KEY (district_id) REFERENCES district(district_id)
);

CREATE TABLE IF NOT EXISTS daily_record (
    record_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id         INTEGER NOT NULL,
    day_number     INTEGER NOT NULL,
    susceptible    INTEGER NOT NULL DEFAULT 0,
    exposed        INTEGER NOT NULL DEFAULT 0,
    infected       INTEGER NOT NULL DEFAULT 0,
    hospitalised   INTEGER NOT NULL DEFAULT 0,
    recovered      INTEGER NOT NULL DEFAULT 0,
    deceased       INTEGER NOT NULL DEFAULT 0,
    new_infections INTEGER NOT NULL DEFAULT 0,
    effective_r    REAL    NOT NULL DEFAULT 0.0,
    beds_occupied  INTEGER NOT NULL DEFAULT 0,
    over_capacity  INTEGER NOT NULL DEFAULT 0 CHECK (over_capacity IN (0,1)),
    UNIQUE (run_id, day_number),
    FOREIGN KEY (run_id) REFERENCES simulation_run(run_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS intervention (
    intervention_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id           INTEGER NOT NULL,
    intervention_type TEXT   NOT NULL
                             CHECK (intervention_type IN ('LOCKDOWN','MASK_MANDATE',
                                                          'VACCINATION_DRIVE','CONTACT_TRACING')),
    name             TEXT    NOT NULL,
    start_day        INTEGER NOT NULL CHECK (start_day >= 0),
    end_day          INTEGER NOT NULL,
    intensity        REAL    NOT NULL CHECK (intensity BETWEEN 0 AND 1),
    cost_per_day_rm  REAL    NOT NULL DEFAULT 0,
    doses_per_day    INTEGER,
    tracing_capacity INTEGER,
    CHECK (end_day >= start_day),
    FOREIGN KEY (run_id) REFERENCES simulation_run(run_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS event_log (
    log_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id      INTEGER NOT NULL,
    day_number  INTEGER NOT NULL,
    event_type  TEXT    NOT NULL,
    description TEXT    NOT NULL,
    logged_at   TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (run_id) REFERENCES simulation_run(run_id) ON DELETE CASCADE
);

-- ---------- Indexes ----------

CREATE INDEX IF NOT EXISTS idx_person_run      ON person(run_id);
CREATE INDEX IF NOT EXISTS idx_person_state    ON person(health_state);
CREATE INDEX IF NOT EXISTS idx_person_district ON person(district_id);
CREATE INDEX IF NOT EXISTS idx_daily_run_day   ON daily_record(run_id, day_number);
CREATE INDEX IF NOT EXISTS idx_interv_run      ON intervention(run_id);
CREATE INDEX IF NOT EXISTS idx_event_run_day   ON event_log(run_id, day_number);

-- ---------- Convenience view (used by the Analysis tab) ----------

CREATE VIEW IF NOT EXISTS v_run_summary AS
SELECT  r.run_id,
        r.run_name,
        p.name                        AS pathogen_name,
        r.population_size,
        r.total_days,
        -- Peak prevalence of active infection: infected + hospitalised, matching
        -- OutbreakAnalyser.peakInfections()/HealthState.isInfectious().
        MAX(d.infected + d.hospitalised) AS peak_infections,
        MAX(d.beds_occupied)          AS peak_beds,
        MAX(d.deceased)               AS total_deaths,
        SUM(d.new_infections)         AS total_infections,
        SUM(d.over_capacity)          AS days_over_capacity,
        r.status
FROM simulation_run r
JOIN pathogen p ON p.pathogen_id = r.pathogen_id
LEFT JOIN daily_record d ON d.run_id = r.run_id
GROUP BY r.run_id;

-- ============================================================
-- SEED DATA  (DatabaseManager.seedIfEmpty() runs this section)
-- ============================================================

INSERT OR IGNORE INTO pathogen
    (name, r0, incubation_days, infectious_days, hospitalisation_rate,
     mortality_rate, vaccine_effectiveness, description)
VALUES
    ('COVID-19 (Delta-like)', 5.1, 4, 10, 0.055, 0.018, 0.85,
     'High transmissibility respiratory virus with significant hospitalisation burden.'),
    ('Seasonal Influenza',    1.4, 2,  6, 0.012, 0.001, 0.60,
     'Endemic respiratory illness; low mortality but high annual case volume.'),
    ('Measles',               14.0, 11, 8, 0.200, 0.002, 0.97,
     'Extremely transmissible; vaccine-preventable. Illustrates herd-immunity thresholds.'),
    ('Novel Pathogen X',      3.0, 6, 12, 0.090, 0.045, 0.40,
     'Hypothetical emerging pathogen used for preparedness planning (SDG Target 3.d).');

INSERT OR IGNORE INTO district
    (district_id, name, population, density_factor, hospital_capacity)
VALUES
    ('KL-CENTRAL', 'Kuala Lumpur City Centre', 4000, 1.65,  90),
    ('PJ-URBAN',   'Petaling Jaya',            3000, 1.25,  70),
    ('SHAH-SUB',   'Shah Alam Suburbs',        2000, 0.95,  45),
    ('RURAL-N',    'Northern Rural Zone',      1000, 0.55,  15);
