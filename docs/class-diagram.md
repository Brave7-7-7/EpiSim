# Class Diagram — `com.episim.model`

The full domain model hierarchy: two parallel abstract-class hierarchies (`Person`, `Intervention`)
demonstrating inheritance and polymorphism, one shared interface (`Reportable`) implemented by two
otherwise-unrelated classes demonstrating interface polymorphism, one enum carrying behaviour, and the
supporting value/DTO classes used by persistence and the GUI.

```mermaid
classDiagram
    class Person {
        <<abstract>>
        -int id
        -String fullName
        -int age
        -String districtId
        -HealthState healthState
        -int daysInCurrentState
        -boolean vaccinated
        -double immunityLevel
        +getExposureMultiplier() double*
        +getSeverityMultiplier() double*
        +getRoleLabel() String*
        +transitionTo(HealthState) void
        +toString() String
    }

    class Citizen {
        +getExposureMultiplier() double
        +getSeverityMultiplier() double
        +getRoleLabel() String
    }

    class HealthcareWorker {
        -boolean hasPPE
        -String hospitalAssigned
        +getExposureMultiplier() double
        +getSeverityMultiplier() double
        +getRoleLabel() String
    }

    class ElderlyResident {
        -String careHomeName
        +getExposureMultiplier() double
        +getSeverityMultiplier() double
        +getRoleLabel() String
    }

    Person <|-- Citizen
    Person <|-- HealthcareWorker
    Person <|-- ElderlyResident

    class Intervention {
        <<abstract>>
        -int id
        -int runId
        -String name
        -int startDay
        -int endDay
        -double intensity
        -double costPerDayRM
        -boolean active
        +transmissionModifier() double*
        +severityModifier() double*
        +getDescription() String*
        +isActiveOn(int day) boolean
        +totalCost() double
    }

    class Lockdown {
        +transmissionModifier() double
        +severityModifier() double
        +getDescription() String
    }

    class MaskMandate {
        +transmissionModifier() double
        +severityModifier() double
        +getDescription() String
    }

    class VaccinationDrive {
        -int dosesPerDay
        +transmissionModifier() double
        +severityModifier() double
        +getDescription() String
    }

    class ContactTracing {
        -int tracingCapacityPerDay
        +transmissionModifier() double
        +severityModifier() double
        +getDescription() String
    }

    Intervention <|-- Lockdown
    Intervention <|-- MaskMandate
    Intervention <|-- VaccinationDrive
    Intervention <|-- ContactTracing

    class Reportable {
        <<interface>>
        +getReportTitle() String
        +toReportLine() String
    }

    class District {
        -String id
        -String name
        -int population
        -double densityFactor
        -int hospitalCapacity
        -int designHospitalCapacity
        -List~Person~ residents
        +addResident(Person) void
        +stateBreakdown() Map~HealthState,Integer~
        +occupiedBeds() int
        +isHospitalOverwhelmed() boolean
        +getReportTitle() String
        +toReportLine() String
        +scaleHospitalCapacities(Collection~District~, int)$ void
    }

    class DailyRecord {
        -int id
        -int runId
        -int dayNumber
        -int susceptible
        -int exposed
        -int infected
        -int hospitalised
        -int recovered
        -int deceased
        -int newInfections
        -double effectiveR
        -int bedsOccupied
        -boolean overCapacity
        +totalAlive() int
        +getReportTitle() String
        +toReportLine() String
    }

    Reportable <|.. District
    Reportable <|.. DailyRecord

    class HealthState {
        <<enumeration>>
        SUSCEPTIBLE
        EXPOSED
        INFECTED
        HOSPITALISED
        RECOVERED
        DECEASED
        -String label
        -Color color
        +isInfectious() boolean
    }

    Person "1" --> "1" HealthState : healthState
    District "1" o-- "*" Person : residents

    class Pathogen {
        -int id
        -String name
        -double r0
        -int incubationDays
        -int infectiousDays
        -double hospitalisationRate
        -double mortalityRate
        -double vaccineEffectiveness
        -String description
        +perContactTransmissionProbability() double
    }

    class SimulationRun {
        -int id
        -String runName
        -int pathogenId
        -int populationSize
        -int totalDays
        -int seedInfections
        -long randomSeed
        -String startedAt
        -String completedAt
        -String status
        -String notes
    }

    class RunSummary {
        -int runId
        -String runName
        -String pathogenName
        -int populationSize
        -int totalDays
        -int peakInfections
        -int peakBeds
        -int totalDeaths
        -int totalInfections
        -int daysOverCapacity
        -String status
    }

    class EventLogEntry {
        -int dayNumber
        -String eventType
        -String description
        -String loggedAt
    }

    SimulationRun "1" --> "1" Pathogen : pathogenId
```

## Notes on the design

- **Two parallel abstract hierarchies** (`Person`/`Intervention`) are the two required abstraction
  points: each defines abstract methods that every concrete subclass must implement differently
  (`getExposureMultiplier()`/`getSeverityMultiplier()`/`getRoleLabel()`; `transmissionModifier()`/
  `severityModifier()`/`getDescription()`), while sharing concrete behaviour in the base class
  (`transitionTo()`, `isActiveOn()`, `totalCost()`).
- **`Reportable`** is implemented by `District` and `DailyRecord` — two classes with nothing else in
  common — so that `TextReportWriter` can loop over a single `List<Reportable>` mixing both kinds and
  call `toReportLine()` polymorphically without any `instanceof` check.
- **`SimulationEngine`** (in `com.episim.engine`, not shown here — this diagram is the `model` package
  only) holds `List<Person>` and `List<Intervention>` and only ever programs against the abstract
  superclass references; the concrete subclass behaviour is resolved at runtime.
