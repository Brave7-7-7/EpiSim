package com.episim.dao;

import com.episim.model.RunSummary;
import com.episim.model.SimulationRun;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SimulationRunDao implements Dao<SimulationRun> {

    @Override
    public void insert(SimulationRun run) throws SQLException {
        // PreparedStatement with bound parameters throughout — never string-concatenated SQL — to prevent SQL injection.
        String sql = "INSERT INTO simulation_run "
                + "(run_name, pathogen_id, population_size, total_days, seed_infections, random_seed, "
                + "completed_at, status, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, run.getRunName());
            ps.setInt(2, run.getPathogenId());
            ps.setInt(3, run.getPopulationSize());
            ps.setInt(4, run.getTotalDays());
            ps.setInt(5, run.getSeedInfections());
            ps.setLong(6, run.getRandomSeed());
            ps.setString(7, run.getCompletedAt());
            ps.setString(8, run.getStatus());
            ps.setString(9, run.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    run.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert simulation run '" + run.getRunName() + "'", e);
        }
    }

    @Override
    public Optional<SimulationRun> findById(int id) throws SQLException {
        String sql = "SELECT * FROM simulation_run WHERE run_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find simulation run with id " + id, e);
        }
    }

    @Override
    public List<SimulationRun> findAll() throws SQLException {
        String sql = "SELECT * FROM simulation_run ORDER BY started_at DESC";
        List<SimulationRun> runs = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                runs.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load simulation runs", e);
        }
        return runs;
    }

    @Override
    public void update(SimulationRun run) throws SQLException {
        String sql = "UPDATE simulation_run SET run_name = ?, pathogen_id = ?, population_size = ?, "
                + "total_days = ?, seed_infections = ?, random_seed = ?, completed_at = ?, status = ?, "
                + "notes = ? WHERE run_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, run.getRunName());
            ps.setInt(2, run.getPathogenId());
            ps.setInt(3, run.getPopulationSize());
            ps.setInt(4, run.getTotalDays());
            ps.setInt(5, run.getSeedInfections());
            ps.setLong(6, run.getRandomSeed());
            ps.setString(7, run.getCompletedAt());
            ps.setString(8, run.getStatus());
            ps.setString(9, run.getNotes());
            ps.setInt(10, run.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update simulation run with id " + run.getId(), e);
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM simulation_run WHERE run_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete simulation run with id " + id, e);
        }
    }

    /**
     * Sets status (and, when the run has ended, completed_at) on a connection the caller owns and will
     * commit/rollback itself — used by SimulationEngine's end-of-run finalisation transaction, and to
     * mark an aborted run without deleting its row.
     */
    public void updateStatus(int runId, String status, String completedAt, Connection conn) throws SQLException {
        String sql = "UPDATE simulation_run SET status = ?, completed_at = ? WHERE run_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, completedAt);
            ps.setInt(3, runId);
            ps.executeUpdate();
        }
    }

    /** Reads the v_run_summary view for the Analysis tab's run list. */
    public List<RunSummary> findAllSummaries() throws SQLException {
        String sql = "SELECT * FROM v_run_summary ORDER BY run_id DESC";
        List<RunSummary> summaries = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                summaries.add(new RunSummary(
                        rs.getInt("run_id"),
                        rs.getString("run_name"),
                        rs.getString("pathogen_name"),
                        rs.getInt("population_size"),
                        rs.getInt("total_days"),
                        rs.getInt("peak_infections"),
                        rs.getInt("peak_beds"),
                        rs.getInt("total_deaths"),
                        rs.getInt("total_infections"),
                        rs.getInt("days_over_capacity"),
                        rs.getString("status")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load run summaries", e);
        }
        return summaries;
    }

    private SimulationRun mapRow(ResultSet rs) throws SQLException {
        return new SimulationRun(
                rs.getInt("run_id"),
                rs.getString("run_name"),
                rs.getInt("pathogen_id"),
                rs.getInt("population_size"),
                rs.getInt("total_days"),
                rs.getInt("seed_infections"),
                rs.getLong("random_seed"),
                rs.getString("started_at"),
                rs.getString("completed_at"),
                rs.getString("status"),
                rs.getString("notes"));
    }
}
