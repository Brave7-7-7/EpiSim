package com.episim.dao;

import com.episim.model.EventLogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only audit trail of notable simulation events (interventions
 * toggled, hospital overwhelmed, run completed, etc).
 */
public class EventLogDao {

    public void log(int runId, int day, String type, String description) {
        // PreparedStatement with bound parameters throughout — never string-concatenated SQL — to prevent SQL injection.
        String sql = "INSERT INTO event_log (run_id, day_number, event_type, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, runId);
            ps.setInt(2, day);
            ps.setString(3, type);
            ps.setString(4, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to log event '" + type + "' for run " + runId, e);
        }
    }

    /** Most recent event_log entries for a run, newest first — feeds the Run History detail pane. */
    public List<EventLogEntry> findByRun(int runId, int limit) throws SQLException {
        String sql = "SELECT day_number, event_type, description, logged_at FROM event_log "
                + "WHERE run_id = ? ORDER BY log_id DESC LIMIT ?";
        List<EventLogEntry> entries = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, runId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new EventLogEntry(rs.getInt("day_number"), rs.getString("event_type"),
                            rs.getString("description"), rs.getString("logged_at")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load event log entries for run " + runId, e);
        }
        return entries;
    }
}
