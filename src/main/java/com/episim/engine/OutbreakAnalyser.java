package com.episim.engine;

import com.episim.model.DailyRecord;
import com.episim.model.Intervention;
import com.episim.util.SimConstants;

import java.util.List;

/**
 * Stateless statistics and plain-English interpretation over a completed
 * (or in-progress) run's daily history.
 */
public final class OutbreakAnalyser {

    private OutbreakAnalyser() {
    }

    // Peak prevalence uses infected + hospitalised, not infected alone: those are exactly the two
    // states HealthState.isInfectious() counts as infectious, and the same two states
    // SimulationEngine.computeForceOfInfectionByDistrict() counts when it derives each district's
    // force of infection. Keeping this definition aligned with the engine matters — otherwise the
    // "peak" reported here could describe a different population than the one that actually drove
    // transmission during the run.
    public static int peakInfections(List<DailyRecord> history) {
        return history.stream().mapToInt(r -> r.getInfected() + r.getHospitalised()).max().orElse(0);
    }

    public static int peakDay(List<DailyRecord> history) {
        int peak = peakInfections(history);
        return history.stream()
                .filter(r -> r.getInfected() + r.getHospitalised() == peak)
                .mapToInt(DailyRecord::getDayNumber)
                .findFirst()
                .orElse(0);
    }

    /**
     * Cumulative incidence: the proportion of the population that was ever infected over the course of
     * the run. Sums newInfections rather than reading any point-in-time state column, so someone who
     * has since recovered or died is still counted.
     */
    public static double attackRate(List<DailyRecord> history, int populationSize) {
        if (populationSize <= 0) {
            return 0.0;
        }
        long totalInfections = history.stream().mapToLong(DailyRecord::getNewInfections).sum();
        return (double) totalInfections / populationSize;
    }

    /** Deaths as a proportion of everyone who was ever infected (cumulative cases, guarding divide-by-zero). */
    public static double caseFatalityRate(List<DailyRecord> history) {
        if (history.isEmpty()) {
            return 0.0;
        }
        long totalInfections = history.stream().mapToLong(DailyRecord::getNewInfections).sum();
        if (totalInfections == 0) {
            return 0.0;
        }
        int finalDeceased = history.get(history.size() - 1).getDeceased();
        return (double) finalDeceased / totalInfections;
    }

    public static long daysHospitalOverCapacity(List<DailyRecord> history) {
        return history.stream().filter(DailyRecord::isOverCapacity).count();
    }

    /** Highest simultaneous hospital bed occupancy reached during the run — the health-system-capacity metric for SDG Target 3.d. */
    public static int peakHospitalOccupancy(List<DailyRecord> history) {
        return history.stream().mapToInt(DailyRecord::getBedsOccupied).max().orElse(0);
    }

    public static double totalInterventionCost(List<Intervention> interventions) {
        return interventions.stream().mapToDouble(Intervention::totalCost).sum();
    }

    /**
     * A short plain-English public-health interpretation of the run. This text is intended for report
     * exports as well as on-screen display, so — like other persisted/exported output — it is formatted
     * with SimConstants.DATA_LOCALE rather than the machine's default locale.
     */
    public static String generateNarrativeSummary(List<DailyRecord> history, List<Intervention> interventions,
                                                    int populationSize) {
        if (history.isEmpty()) {
            return "No simulation data is available to summarise.";
        }

        int peakDay = peakDay(history);
        int peakInfections = peakInfections(history);
        double attackRate = attackRate(history, populationSize);
        double caseFatalityRate = caseFatalityRate(history);
        long overCapacityDays = daysHospitalOverCapacity(history);
        double interventionCost = totalInterventionCost(interventions);
        int totalDeceased = history.get(history.size() - 1).getDeceased();

        StringBuilder summary = new StringBuilder();
        summary.append(String.format(SimConstants.DATA_LOCALE,
                "The outbreak peaked on day %d with %d concurrent infections.", peakDay, peakInfections));
        summary.append(String.format(SimConstants.DATA_LOCALE,
                " An estimated %.1f%% of the population was infected over the course of the run, resulting in %d deaths (a case fatality rate of %.2f%%).",
                attackRate * 100, totalDeceased, caseFatalityRate * 100));

        if (overCapacityDays > 0) {
            summary.append(String.format(SimConstants.DATA_LOCALE,
                    " Hospital capacity was exceeded for %d day%s, which increased the fatality rate among hospitalised patients by an estimated 80%%.",
                    overCapacityDays, overCapacityDays == 1 ? "" : "s"));
        } else {
            summary.append(" Hospital capacity was never exceeded during the run.");
        }

        if (!interventions.isEmpty()) {
            summary.append(String.format(SimConstants.DATA_LOCALE,
                    " %d public-health intervention%s were deployed at a total cost of RM%.2f.",
                    interventions.size(), interventions.size() == 1 ? "" : "s", interventionCost));
        } else {
            summary.append(" No public-health interventions were deployed during this run.");
        }

        summary.append(" These results are reproducible: the same configuration and random seed will regenerate an identical outbreak trajectory.");

        return summary.toString();
    }
}
