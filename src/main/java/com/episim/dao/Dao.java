package com.episim.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract implemented by every entity-specific DAO.
 */
public interface Dao<T> {

    void insert(T t) throws SQLException;

    Optional<T> findById(int id) throws SQLException;

    List<T> findAll() throws SQLException;

    void update(T t) throws SQLException;

    void delete(int id) throws SQLException;
}
