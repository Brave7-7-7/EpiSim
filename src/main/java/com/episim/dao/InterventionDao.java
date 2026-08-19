package com.episim.dao;

import com.episim.model.ContactTracing;
import com.episim.model.Intervention;
import com.episim.model.Lockdown;
import com.episim.model.MaskMandate;
import com.episim.model.VaccinationDrive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD access to the {@code intervention} table, including polymorphic reconstruction of concrete subclasses. */
public class InterventionDao implements Dao<Intervention> {

    private static final String INSERT_SQL = "INSERT INTO intervention "
            + "(run_id, intervention_type, name, start_day, end_day, intensity, cost_per_day_rm, "
            + "doses_per_day, tracing_capacity) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** {@inheritDoc} */
    @Override
    public void insert(Intervention intervention) throws SQLException {
        // PreparedStatement with bound parameters throughout — never string-concatenated SQL — to prevent SQL injection.
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, intervention);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    intervention.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert intervention '" + intervention.getName() + "'", e);
        }
    }

    /**
     * Inserts a run's interventions on a connection the caller owns and will commit/rollback itself —
     * used by SimulationEngine's end-of-run finalisation transaction. The intervention count per run is
     * small, so plain per-row executeUpdate() (rather than executeBatch()) is used, letting
     * getGeneratedKeys() report each row's id directly without needing a last_insert_rowid() workaround.
     *
     * @param interventions the interventions to insert; each has its generated id written back onto it
     * @param conn          an open connection the caller owns; this method does not commit or close it
     * @throws SQLException if any insert fails
     */
    public void insertBatch(List<Intervention> interventions, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            for (Intervention intervention : interventions) {
                bind(ps, intervention);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        intervention.setId(keys.getInt(1));
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Intervention> findById(int id) throws SQLException {
        String sql = "SELECT * FROM intervention WHERE intervention_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find intervention with id " + id, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Intervention> findAll() throws SQLException {
        String sql = "SELECT * FROM intervention ORDER BY start_day";
        List<Intervention> interventions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                interventions.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load interventions", e);
        }
        return interventions;
    }

    /**
     * All interventions configured for one run, reconstructed as their concrete subclasses.
     *
     * @param runId id of the run to fetch interventions for
     * @return every intervention for that run, ordered by start day
     * @throws SQLException if the query fails
     */
    public List<Intervention> findByRun(int runId) throws SQLException {
        String sql = "SELECT * FROM intervention WHERE run_id = ? ORDER BY start_day";
        List<Intervention> interventions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    interventions.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load interventions for run " + runId, e);
        }
        return interventions;
    }

    /** {@inheritDoc} */
    @Override
    public void update(Intervention intervention) throws SQLException {
        String sql = "UPDATE intervention SET run_id = ?, intervention_type = ?, name = ?, start_day = ?, "
                + "end_day = ?, intensity = ?, cost_per_day_rm = ?, doses_per_day = ?, tracing_capacity = ? "
                + "WHERE intervention_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, intervention);
            ps.setInt(10, intervention.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update intervention with id " + intervention.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM intervention WHERE intervention_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete intervention with id " + id, e);
        }
    }

    /** @return the {@code intervention_type} discriminator value for this intervention's concrete subclass */
    private String interventionType(Intervention intervention) {
        if (intervention instanceof Lockdown) {
            return "LOCKDOWN";
        } else if (intervention instanceof MaskMandate) {
            return "MASK_MANDATE";
        } else if (intervention instanceof VaccinationDrive) {
            return "VACCINATION_DRIVE";
        } else if (intervention instanceof ContactTracing) {
            return "CONTACT_TRACING";
        }
        throw new IllegalArgumentException("Unknown intervention subclass: " + intervention.getClass());
    }

    /** Binds every column, in column order, for both insert and update; subclass-only columns get {@code NULL} when not applicable. */
    private void bind(PreparedStatement ps, Intervention intervention) throws SQLException {
        ps.setInt(1, intervention.getRunId());
        ps.setString(2, interventionType(intervention));
        ps.setString(3, intervention.getName());
        ps.setInt(4, intervention.getStartDay());
        ps.setInt(5, intervention.getEndDay());
        ps.setDouble(6, intervention.getIntensity());
        ps.setDouble(7, intervention.getCostPerDayRM());

        if (intervention instanceof VaccinationDrive drive) {
            ps.setInt(8, drive.getDosesPerDay());
        } else {
            ps.setNull(8, Types.INTEGER);
        }

        if (intervention instanceof ContactTracing tracing) {
            ps.setInt(9, tracing.getTracingCapacityPerDay());
        } else {
            ps.setNull(9, Types.INTEGER);
        }
    }

    /**
     * Polymorphic reconstruction: the concrete subclass is chosen from the discriminator column, so the
     * caller receives {@code List<Intervention>} with mixed runtime types.
     *
     * @return the {@link Lockdown}, {@link MaskMandate}, {@link VaccinationDrive}, or {@link ContactTracing}
     *         built from the current row, per its {@code intervention_type}
     */
    private Intervention mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("intervention_id");
        int runId = rs.getInt("run_id");
        String name = rs.getString("name");
        int startDay = rs.getInt("start_day");
        int endDay = rs.getInt("end_day");
        double intensity = rs.getDouble("intensity");
        double costPerDayRM = rs.getDouble("cost_per_day_rm");
        String type = rs.getString("intervention_type");

        return switch (type) {
            case "LOCKDOWN" -> new Lockdown(id, runId, name, startDay, endDay, intensity, costPerDayRM, true);
            case "MASK_MANDATE" -> new MaskMandate(id, runId, name, startDay, endDay, intensity, costPerDayRM, true);
            case "VACCINATION_DRIVE" -> new VaccinationDrive(id, runId, name, startDay, endDay, intensity,
                    costPerDayRM, true, rs.getInt("doses_per_day"));
            case "CONTACT_TRACING" -> new ContactTracing(id, runId, name, startDay, endDay, intensity,
                    costPerDayRM, true, rs.getInt("tracing_capacity"));
            default -> throw new IllegalStateException("Unknown intervention_type in database: " + type);
        };
    }
}
