package com.episim.dao;

import com.episim.model.DailyRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DailyRecordDao implements Dao<DailyRecord> {

    private static final String INSERT_SQL = "INSERT INTO daily_record "
            + "(run_id, day_number, susceptible, exposed, infected, hospitalised, recovered, deceased, "
            + "new_infections, effective_r, beds_occupied, over_capacity) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    @Override
    public void insert(DailyRecord record) throws SQLException {
        // PreparedStatement with bound parameters throughout — never string-concatenated SQL — to prevent SQL injection.
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, record);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    record.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Failed to insert daily record for run " + record.getRunId() + " day " + record.getDayNumber(), e);
        }
    }

    /** Bulk-inserts a run's daily records inside their own single-use transaction — far faster than one-by-one inserts. */
    public void insertBatch(List<DailyRecord> records) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            try {
                insertBatch(records, conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new DataAccessException("Batch insert of daily records failed; transaction rolled back", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to open transaction for daily record batch insert", e);
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
    }

    /**
     * Same batch insert, but running on a connection the caller owns and will commit/rollback itself —
     * used by SimulationEngine's end-of-run finalisation, which must flush the last records, update
     * persons, insert interventions, and mark the run COMPLETED all inside one transaction.
     */
    public void insertBatch(List<DailyRecord> records, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            for (DailyRecord record : records) {
                bind(ps, record);
                ps.addBatch();
            }
            ps.executeBatch();

            // The SQLite JDBC driver does not reliably return one generated key per row from
            // getGeneratedKeys() after executeBatch(), so ids are back-computed instead: record_id is
            // INTEGER PRIMARY KEY AUTOINCREMENT, which guarantees strictly sequential, gap-free values
            // within one transaction on one connection, so the batch occupies [lastRowId - n + 1, lastRowId].
            if (!records.isEmpty()) {
                try (Statement idStatement = conn.createStatement();
                     ResultSet rs = idStatement.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        long lastId = rs.getLong(1);
                        long firstId = lastId - records.size() + 1;
                        for (int i = 0; i < records.size(); i++) {
                            records.get(i).setId((int) (firstId + i));
                        }
                    }
                }
            }
        }
    }

    @Override
    public Optional<DailyRecord> findById(int id) throws SQLException {
        String sql = "SELECT * FROM daily_record WHERE record_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find daily record with id " + id, e);
        }
    }

    @Override
    public List<DailyRecord> findAll() throws SQLException {
        String sql = "SELECT * FROM daily_record ORDER BY run_id, day_number";
        List<DailyRecord> records = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load daily records", e);
        }
        return records;
    }

    /** All daily records for one run, ordered by day — the series the charts plot. */
    public List<DailyRecord> findByRun(int runId) throws SQLException {
        String sql = "SELECT * FROM daily_record WHERE run_id = ? ORDER BY day_number";
        List<DailyRecord> records = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load daily records for run " + runId, e);
        }
        return records;
    }

    @Override
    public void update(DailyRecord record) throws SQLException {
        String sql = "UPDATE daily_record SET run_id = ?, day_number = ?, susceptible = ?, exposed = ?, "
                + "infected = ?, hospitalised = ?, recovered = ?, deceased = ?, new_infections = ?, "
                + "effective_r = ?, beds_occupied = ?, over_capacity = ? WHERE record_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, record);
            ps.setInt(13, record.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update daily record with id " + record.getId(), e);
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM daily_record WHERE record_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete daily record with id " + id, e);
        }
    }

    private void bind(PreparedStatement ps, DailyRecord record) throws SQLException {
        ps.setInt(1, record.getRunId());
        ps.setInt(2, record.getDayNumber());
        ps.setInt(3, record.getSusceptible());
        ps.setInt(4, record.getExposed());
        ps.setInt(5, record.getInfected());
        ps.setInt(6, record.getHospitalised());
        ps.setInt(7, record.getRecovered());
        ps.setInt(8, record.getDeceased());
        ps.setInt(9, record.getNewInfections());
        ps.setDouble(10, record.getEffectiveR());
        ps.setInt(11, record.getBedsOccupied());
        ps.setInt(12, record.isOverCapacity() ? 1 : 0);
    }

    private DailyRecord mapRow(ResultSet rs) throws SQLException {
        return new DailyRecord(
                rs.getInt("record_id"),
                rs.getInt("run_id"),
                rs.getInt("day_number"),
                rs.getInt("susceptible"),
                rs.getInt("exposed"),
                rs.getInt("infected"),
                rs.getInt("hospitalised"),
                rs.getInt("recovered"),
                rs.getInt("deceased"),
                rs.getInt("new_infections"),
                rs.getDouble("effective_r"),
                rs.getInt("beds_occupied"),
                rs.getInt("over_capacity") == 1);
    }
}
