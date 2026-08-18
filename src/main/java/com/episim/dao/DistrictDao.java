package com.episim.dao;

import com.episim.model.District;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * District's primary key is the natural String district_id from schema.sql
 * (e.g. "KL-CENTRAL"), not a generated surrogate integer, so this DAO offers
 * an equivalent String-keyed CRUD API rather than implementing the
 * int-keyed generic {@link Dao} contract.
 */
public class DistrictDao {

    public void insert(District district) throws SQLException {
        // PreparedStatement with bound parameters throughout — never string-concatenated SQL — to prevent SQL injection.
        String sql = "INSERT INTO district (district_id, name, population, density_factor, hospital_capacity) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, district);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert district '" + district.getId() + "'", e);
        }
    }

    public Optional<District> findById(String id) throws SQLException {
        String sql = "SELECT * FROM district WHERE district_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find district with id " + id, e);
        }
    }

    public List<District> findAll() throws SQLException {
        String sql = "SELECT * FROM district ORDER BY name";
        List<District> districts = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                districts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load districts", e);
        }
        return districts;
    }

    public void update(District district) throws SQLException {
        String sql = "UPDATE district SET name = ?, population = ?, density_factor = ?, hospital_capacity = ? "
                + "WHERE district_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, district.getName());
            ps.setInt(2, district.getPopulation());
            ps.setDouble(3, district.getDensityFactor());
            ps.setInt(4, district.getHospitalCapacity());
            ps.setString(5, district.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update district with id " + district.getId(), e);
        }
    }

    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM district WHERE district_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete district with id " + id, e);
        }
    }

    private void bind(PreparedStatement ps, District district) throws SQLException {
        ps.setString(1, district.getId());
        ps.setString(2, district.getName());
        ps.setInt(3, district.getPopulation());
        ps.setDouble(4, district.getDensityFactor());
        ps.setInt(5, district.getHospitalCapacity());
    }

    private District mapRow(ResultSet rs) throws SQLException {
        return new District(
                rs.getString("district_id"),
                rs.getString("name"),
                rs.getInt("population"),
                rs.getDouble("density_factor"),
                rs.getInt("hospital_capacity"));
    }
}
