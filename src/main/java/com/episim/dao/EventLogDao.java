package com.episim.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
}
