# EpiSim — Epidemic Outbreak Simulation & Public Health Response System

A Java Swing desktop application simulating epidemic outbreaks across a set of districts, letting the
user configure public-health interventions (lockdowns, mask mandates, vaccination drives, contact
tracing) and observe their effect on the outbreak's trajectory. Built as an Object-Oriented Programming
project aligned to UN Sustainable Development Goal 3 (Good Health and Well-being), Targets 3.3 (end
epidemics of communicable diseases) and 3.d (strengthen capacity for early warning and risk management).

## Requirements

- Java 17 or later (JDK, not just a JRE)
- Maven 3.8+

No other tools are required — SQLite runs embedded (via `org.xerial:sqlite-jdbc`), so there is no
database server to install or configure.

## Build

```
mvn clean package
```

This compiles the project, runs the full JUnit 5 test suite, and produces a self-contained runnable jar
at `target/episim.jar` (via `maven-shade-plugin`).

## Run

```
java -jar target/episim.jar
```

On first launch this creates `data/episim.db` (a SQLite database file) in the working directory and
seeds it with reference data — pathogens and districts. The database persists across runs; simulation
history accumulates there and is visible in the app's **Run History** tab.

The app window shows the live database's absolute path in its status bar, so you can always see exactly
where your data is being written.

### Resetting the database

To start from a completely clean database (e.g. after a schema change), delete `data/episim.db` and
relaunch — it will be recreated and reseeded automatically.

## Running the tests only

```
mvn test
```

Tests never touch `data/episim.db` — they run against a private in-memory SQLite database
(`DatabaseManager.useInMemoryDatabaseForTests()`), so it's always safe to run the suite without
affecting your saved simulation history.

## Project layout

```
com.episim
├── Main.java        entry point — Nimbus look and feel, DB bootstrap, opens the GUI
├── model            domain classes (Person hierarchy, Pathogen, District, Intervention hierarchy, ...)
├── engine           the simulation itself (SimulationEngine, PopulationGenerator, OutbreakAnalyser)
├── dao              SQLite persistence (one DAO per entity, plus DatabaseManager)
├── io               CSV and plain-text file export/import (CsvExporter, CsvImporter, TextReportWriter)
├── gui              Swing screens: MainDashboard and its five tabs
└── util             SimConstants, Theme, AppConfig
```

## Using the app

1. **Configure a scenario** in the left-hand panel: choose a pathogen, population size, simulation
   length, seed infections, and random seed. Optionally enable one or more interventions with their own
   intensity and active date range.
2. **Start** the run. The Epidemic Curve tab animates day-by-day; Population, Districts, and Analysis
   update as you switch to them. Pause/Step/Abort/Reset control playback.
3. **Analysis tab**: every `OutbreakAnalyser` metric, a plain-English narrative summary, and CSV/text
   export/import — files default to the `exports/` folder (created automatically) and the location is
   remembered in `config.properties` for next time.
4. **Run History tab**: every run ever persisted to SQLite, read straight from the `v_run_summary` SQL
   view. Load a past run back into the other tabs (read-only), or delete one (cascades to its
   people/daily records/interventions/events).

## Configuration file

`config.properties` (created next to the jar on first change) stores a few defaults:

| Key | Meaning |
|---|---|
| `default.population` | Starting value of the Population size spinner |
| `default.days` | Starting value of the Simulation days spinner |
| `export.directory` | Last-used folder for CSV/text exports |
| `autosave.enabled` | Reserved for future use |

It's optional — if absent, sensible built-in defaults are used, and the file is written back
automatically the first time a value changes (e.g. after your first export).

## Further documentation

- [`docs/architecture-diagram.md`](docs/architecture-diagram.md) — package-level dependency diagram and the model layer's isolation
- [`docs/class-diagram.md`](docs/class-diagram.md) — Mermaid class diagram of the full model hierarchy
- [`docs/database-design.md`](docs/database-design.md) — schema, ER description, and example queries
- [`docs/oop-evidence.md`](docs/oop-evidence.md) — rubric requirement → class/method mapping
