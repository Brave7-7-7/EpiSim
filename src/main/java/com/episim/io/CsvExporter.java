package com.episim.io;

import com.episim.model.DailyRecord;
import com.episim.model.Intervention;
import com.episim.model.Person;
import com.episim.model.SimulationRun;
import com.episim.util.SimConstants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes EpiSim data to CSV files, RFC-4180 style: any field containing a comma, double quote, or
 * newline is wrapped in double quotes with internal quotes doubled. Every numeric field is formatted
 * with SimConstants.DATA_LOCALE rather than the machine's default locale — see that class for the full
 * locale policy — since these files are persisted/exported data, not GUI display text.
 */
public final class CsvExporter {

    private CsvExporter() {
    }

    /**
     * @param records the run's day-by-day history, in the order to write them
     * @param file    the destination CSV file, overwritten if it already exists
     * @throws ReportIoException if the file cannot be written
     */
    public static void exportDailyRecords(List<DailyRecord> records, Path file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeLine(writer, "day,susceptible,exposed,infected,hospitalised,recovered,deceased,"
                    + "new_infections,effective_r,beds_occupied,over_capacity");
            for (DailyRecord record : records) {
                writeLine(writer, String.format(SimConstants.DATA_LOCALE, "%d,%d,%d,%d,%d,%d,%d,%d,%.6f,%d,%b",
                        record.getDayNumber(), record.getSusceptible(), record.getExposed(), record.getInfected(),
                        record.getHospitalised(), record.getRecovered(), record.getDeceased(),
                        record.getNewInfections(), record.getEffectiveR(), record.getBedsOccupied(),
                        record.isOverCapacity()));
            }
        } catch (IOException e) {
            throw new ReportIoException("Failed to export daily records to " + file, e);
        }
    }

    /**
     * @param people the population to write, including its polymorphic role label
     * @param file   the destination CSV file, overwritten if it already exists
     * @throws ReportIoException if the file cannot be written
     */
    public static void exportPopulation(List<Person> people, Path file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeLine(writer, "id,name,age,role,district_id,health_state,days_in_state,vaccinated,immunity_level");
            for (Person person : people) {
                writeLine(writer, String.format(SimConstants.DATA_LOCALE, "%d,%s,%d,%s,%s,%s,%d,%b,%.4f",
                        person.getId(), quote(person.getFullName()), person.getAge(),
                        // getRoleLabel() is polymorphic — resolved to whichever concrete Person subclass
                        // this actually is (Citizen/HealthcareWorker/ElderlyResident) at runtime.
                        quote(person.getRoleLabel()),
                        quote(person.getDistrictId()), person.getHealthState().name(),
                        person.getDaysInCurrentState(), person.isVaccinated(), person.getImmunityLevel()));
            }
        } catch (IOException e) {
            throw new ReportIoException("Failed to export population to " + file, e);
        }
    }

    /**
     * Writes the run's metadata as a {@code field,value} table, followed by a blank line and one row
     * per intervention with its computed total cost.
     *
     * @param run           the run to summarise
     * @param interventions the interventions deployed during the run
     * @param file          the destination CSV file, overwritten if it already exists
     * @throws ReportIoException if the file cannot be written
     */
    public static void exportRunSummary(SimulationRun run, List<Intervention> interventions, Path file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeLine(writer, "field,value");
            writeField(writer, "run_id", Integer.toString(run.getId()));
            writeField(writer, "run_name", run.getRunName());
            writeField(writer, "population_size", Integer.toString(run.getPopulationSize()));
            writeField(writer, "total_days", Integer.toString(run.getTotalDays()));
            writeField(writer, "seed_infections", Integer.toString(run.getSeedInfections()));
            writeField(writer, "random_seed", Long.toString(run.getRandomSeed()));
            writeField(writer, "status", run.getStatus());
            writeField(writer, "started_at", run.getStartedAt());
            writeField(writer, "completed_at", run.getCompletedAt());

            writer.newLine();
            writeLine(writer, "intervention_type,name,start_day,end_day,intensity,cost_per_day_rm,total_cost");
            for (Intervention intervention : interventions) {
                writeLine(writer, String.format(SimConstants.DATA_LOCALE, "%s,%s,%d,%d,%.2f,%.2f,%.2f",
                        intervention.getClass().getSimpleName(), quote(intervention.getName()),
                        intervention.getStartDay(), intervention.getEndDay(), intervention.getIntensity(),
                        intervention.getCostPerDayRM(), intervention.totalCost()));
            }
        } catch (IOException e) {
            throw new ReportIoException("Failed to export run summary to " + file, e);
        }
    }

    /** Writes one {@code key,value} row, quoting the value if needed. */
    private static void writeField(BufferedWriter writer, String key, String value) throws IOException {
        writeLine(writer, key + "," + quote(value));
    }

    /** Writes a line followed by the platform line separator. */
    private static void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }

    /** RFC-4180 quoting: wraps in double quotes (doubling any internal quotes) only if needed. */
    private static String quote(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        if (!needsQuoting) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
