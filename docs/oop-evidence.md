# OOP Evidence Map

Every mandatory rubric requirement, mapped to the exact class and method where it is demonstrated in
this codebase. Package prefix `com.episim.` omitted below for brevity.

## Classes & Objects

| Evidence | Class | Notes |
|---|---|---|
| Real-world domain entities modelled as classes | `model.Person`, `model.Pathogen`, `model.District`, `model.Intervention`, `model.DailyRecord`, `model.SimulationRun` | Twenty-plus classes across `model`, `engine`, `dao`, `io`, and `gui`, each with a single clear responsibility |
| Object instantiation & composition | `engine.SimulationEngine` | Holds `List<Person>`, `Map<String,District>`, `List<Intervention>` — a graph of collaborating objects, not a monolithic procedure |
| Constructors enforcing valid initial state | `model.Person(int, String, int, String, HealthState, int, boolean, double)` (protected) and every subclass constructor | Every field is set exactly once, at construction |

## Encapsulation

| Evidence | Class / Method | Notes |
|---|---|---|
| All fields private, accessed only via getters/setters | `model.Person`, `model.Pathogen`, `model.District`, `model.Intervention`, `model.DailyRecord`, `model.SimulationRun` | No public fields exist anywhere in the project — a hard constraint enforced from the domain model's first design |
| Internal collection exposed read-only | `model.District.getResidents()` returns the live `List<Person>`, but the only way to *add* to it is `addResident(Person)` — no `setResidents()` exists | Prevents a caller from swapping the whole roster out from under the district |
| Engine state exposed only as unmodifiable views | `engine.SimulationEngine.getPopulation()`, `getDistricts()`, `getHistory()`, `getInterventions()` | Each wraps the live field in `Collections.unmodifiableList`/`unmodifiableMap` — callers (the GUI) can read but never structurally mutate the engine's internal state |
| Validated mutation through a method, not a raw setter | `model.Person.transitionTo(HealthState)` | Setting `healthState` directly would leave `daysInCurrentState` stale; the only sanctioned way to change state resets both fields together |
| Immutable-after-construction field | `model.District.designHospitalCapacity` (`final`, no setter) | Preserves the original reference-data value even after `hospitalCapacity` is rescaled for a run |

## Inheritance

| Evidence | Superclass | Subclasses |
|---|---|---|
| Person hierarchy | `model.Person` (abstract) | `model.Citizen`, `model.HealthcareWorker`, `model.ElderlyResident` |
| Intervention hierarchy | `model.Intervention` (abstract) | `model.Lockdown`, `model.MaskMandate`, `model.VaccinationDrive`, `model.ContactTracing` |
| Concrete methods inherited unchanged | `Person.transitionTo(HealthState)`, `Intervention.isActiveOn(int)` / `totalCost()` | Defined once in the abstract base, used by every subclass without overriding |
| Subclass-only state | `HealthcareWorker.hasPPE`/`hospitalAssigned`, `ElderlyResident.careHomeName`, `VaccinationDrive.dosesPerDay`, `ContactTracing.tracingCapacityPerDay` | Fields that only make sense for that specific subclass |

## Polymorphism

| Evidence | Location | Notes |
|---|---|---|
| Runtime dispatch through a superclass reference (class polymorphism) | `engine.SimulationEngine.stepOneDay()` — the transmission-modifier loop | Iterates `List<Intervention>` calling `intervention.transmissionModifier()`; the code is annotated `// Runtime polymorphism: the correct subclass implementation... is resolved at runtime` and never uses `instanceof` |
| Runtime dispatch through a superclass reference | `engine.SimulationEngine.applyExposures()`, `applyHospitalisation()`, `applyResolution()` | All call `person.getExposureMultiplier()`/`getSeverityMultiplier()` through the `Person` reference; the engine's `List<Person> population` field is declared as the abstract type throughout |
| Interface polymorphism | `io.TextReportWriter.writeReport(Path, SimulationRun, List<Reportable>, String)` | Loops a single `List<Reportable>` containing both `District` and `DailyRecord` objects — two otherwise-unrelated classes — calling `toReportLine()` on each; commented as the interface-polymorphism demonstration at the call site |
| Polymorphic reconstruction from persisted data | `dao.PersonDao.mapRow(ResultSet)` | Chooses `Citizen`/`HealthcareWorker`/`ElderlyResident` from the `person_type` discriminator column, so `PersonDao.findAll()` returns a `List<Person>` of genuinely mixed runtime types |
| Polymorphic reconstruction from persisted data | `dao.InterventionDao.mapRow(ResultSet)` | Same pattern, keyed on `intervention_type` |
| Overridden `toString()` | `model.Person.toString()` | Uses `getClass().getSimpleName()` and the polymorphic `getRoleLabel()` |

## Abstraction

| Evidence | Class | Notes |
|---|---|---|
| Abstract class with abstract + concrete methods | `model.Person` | Abstract: `getExposureMultiplier()`, `getSeverityMultiplier()`, `getRoleLabel()`. Concrete: `transitionTo(HealthState)` |
| Abstract class with abstract + concrete methods | `model.Intervention` | Abstract: `transmissionModifier()`, `severityModifier()`, `getDescription()`. Concrete: `isActiveOn(int)`, `totalCost()` |
| Generic interface hiding persistence detail | `dao.Dao<T>` | `insert`/`findById`/`findAll`/`update`/`delete` — every entity DAO implements this without exposing SQL to its callers |
| Complex process hidden behind a simple call | `engine.SimulationEngine.stepOneDay()` | Callers (the GUI, tests) never see the force-of-infection maths, hospitalisation queueing, or transaction handling inside |
| Complex statistics hidden behind simple calls | `engine.OutbreakAnalyser` (all methods `static`) | `peakInfections()`, `attackRate()`, `caseFatalityRate()`, `generateNarrativeSummary()` — callers pass a `List<DailyRecord>` and get an answer, not a formula |

## Collections

| Evidence | Class / Field | Type |
|---|---|---|
| List | `model.District.residents` | `List<Person>` |
| Map | `engine.SimulationEngine.districts` | `Map<String, District>` (`HashMap`) |
| Map with enum keys | `model.District.stateBreakdown()` | `Map<HealthState, Integer>`, built with `HashMap.merge()` |
| Nested collections | `dao.PersonDao.countByStateAndType(int)` | `Map<String, Map<HealthState, Integer>>` |
| Queue | `engine.SimulationEngine.admissionQueue` | `Deque<Person>` (`ArrayDeque`), used with genuine `poll()`/`offer()` FIFO semantics in `admitFromQueue()` — patients waiting for a hospital bed |
| Set (dedup) | `engine.SimulationEngine.applyHospitalisation()` — `loggedOverwhelmedDistrictsToday` | `Set<String>` (`HashSet`), ensures one `HOSPITAL_OVERWHELMED` event per district per day rather than one per patient |
| Streams over collections | `engine.OutbreakAnalyser`, `engine.SimulationEngine.computeForceOfInfectionByDistrict()` | `Collectors.groupingBy`, `mapToInt`/`mapToLong`/`sum`/`max` throughout |

## File Handling

| Evidence | Class / Method | Notes |
|---|---|---|
| CSV write, RFC-4180 quoting | `io.CsvExporter.exportDailyRecords/exportPopulation/exportRunSummary` | `BufferedWriter` in try-with-resources; string fields run through a private `quote()` helper that wraps in `"..."` and doubles internal quotes when a comma/quote/newline is present |
| CSV read with line-numbered error reporting | `io.CsvImporter.importDailyRecords(Path)` | `BufferedReader`, explicit line counter; a malformed row's `IOException` message names the exact file and line number |
| Header validation | `io.CsvImporter.importDailyRecords(Path)` | Compares the file's first line against `EXPECTED_HEADER` and throws a descriptive `IOException` naming both the expected and actual header if they don't match |
| Plain-text report writing | `io.TextReportWriter.writeReport(Path, SimulationRun, List<Reportable>, String)` | `Files.writeString()`; fixed-width header block, section rules, right-aligned numeric fields |
| `java.util.Properties` read | `util.AppConfig.load()` | `Properties.load(InputStream)` from `config.properties`, falling back to built-in defaults when the file or a key is absent |
| `java.util.Properties` write | `util.AppConfig.save()` (private, called from every setter) | `Properties.store(OutputStream, String)` — writes the file back to disk on every configuration change, e.g. after a successful export remembers the chosen directory |
| Classpath resource read | `dao.DatabaseManager.readSchemaResource()` | `getResourceAsStream("/schema.sql")`, read fully via `InputStream.readAllBytes()` |

## Persistence beyond flat files (bonus context for the report)

The above is deliberately scoped to what the rubric's seven categories ask for. The project also has a
full SQLite persistence layer (`dao` package, one DAO per entity, `PRAGMA foreign_keys = ON`,
`PreparedStatement`-only SQL, batch transactions) documented separately in
[`docs/database-design.md`](database-design.md), and a Swing GUI (`gui` package, five tabs, all
database/simulation work dispatched through `SwingWorker`) — neither of which is one of the seven listed
categories, but both are additional demonstrations of the same OOP principles applied at scale.
