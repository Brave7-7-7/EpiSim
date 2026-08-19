package com.episim.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Bootstraps and owns the SQLite database file. Every DAO obtains its own
 * connection from {@link #getConnection()} and closes it via
 * try-with-resources, so this class holds no long-lived state beyond a
 * reference used purely as a safety net for {@link #close()} on shutdown.
 */
public final class DatabaseManager {

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:data/episim.db";

    // Overridable rather than a constant so tests can redirect persistence away from the real
    // data/episim.db file — see useInMemoryDatabaseForTests(). Production code never calls setDbUrl().
    private static String dbUrl = DEFAULT_DB_URL;

    private static Connection lastOpenedConnection;

    // Held open only in in-memory test mode: SQLite's ":memory:" database is scoped to the connection
    // that created it, so without a connection kept alive for the shared cache's lifetime, every
    // subsequent getConnection() call (each DAO opens its own) would see a brand new, empty database.
    private static Connection inMemoryKeepAliveConnection;

    private DatabaseManager() {
    }

    /** Points persistence at an in-memory SQLite database and re-initialises the schema on it. Test-only. */
    public static void useInMemoryDatabaseForTests() {
        close();
        dbUrl = "jdbc:sqlite:file::memory:?cache=shared";
        try {
            inMemoryKeepAliveConnection = DriverManager.getConnection(dbUrl);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to open the in-memory test database", e);
        }
        initialise();
    }

    /**
     * Opens a fresh connection and enables foreign-key enforcement on it —
     * SQLite disables FK enforcement by default on every new connection, so
     * this pragma is mandatory on each call, not a one-time setup step.
     *
     * @return a new, open connection with {@code PRAGMA foreign_keys = ON} already applied
     * @throws DataAccessException if the connection cannot be opened — the message explains the likely
     *                             cause (file locked by another program, missing/unwritable directory,
     *                             etc.) rather than exposing a raw driver stack trace
     */
    public static Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(dbUrl);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            lastOpenedConnection = connection;
            return connection;
        } catch (SQLException e) {
            throw new DataAccessException(describeConnectionFailure(e), e);
        }
    }

    /**
     * Turns a raw {@link SQLException} from opening the database into a message a non-technical user can
     * act on, rather than a JDBC stack trace. SQLite's own exception text already distinguishes these
     * cases reasonably well (e.g. "database is locked"), so this only adds the likely cause and a
     * suggested next step on top of it.
     */
    private static String describeConnectionFailure(SQLException e) {
        String detail = e.getMessage() == null ? "" : e.getMessage().toLowerCase(java.util.Locale.ROOT);
        String path = Path.of("data/episim.db").toAbsolutePath().toString();

        if (detail.contains("locked") || detail.contains("busy")) {
            return "The database file appears to be locked:\n" + path
                    + "\n\nThis usually means it's already open in another program — for example, a "
                    + "second copy of EpiSim, or a SQLite browser tool. Close that program and try again.";
        }
        if (detail.contains("unable to open database file") || detail.contains("no such file or directory")) {
            return "The database file could not be opened:\n" + path
                    + "\n\nCheck that the 'data' folder exists and that this program has permission to "
                    + "read and write there.";
        }
        return "Could not open the database at:\n" + path + "\n\nDetails: " + e.getMessage();
    }

    /**
     * Creates the {@code data} directory (if the database is file-backed), executes {@code schema.sql}
     * against it, and seeds reference data if the pathogen table is empty. Safe to call on every launch
     * — every statement is idempotent ({@code CREATE ... IF NOT EXISTS}, {@code INSERT OR IGNORE}).
     *
     * @throws DataAccessException if the schema cannot be read, the database cannot be opened, or the
     *                             schema fails to execute
     */
    public static void initialise() {
        boolean fileBacked = !dbUrl.contains(":memory:");
        if (fileBacked) {
            try {
                Files.createDirectories(Path.of("data"));
            } catch (IOException e) {
                throw new DataAccessException("Unable to create the data directory", e);
            }
        }

        List<String> statements = splitStatements(readSchemaResource());

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute schema.sql", e);
        }

        seedIfEmpty();
    }

    /**
     * Guards against a blank database: seeds reference data only if pathogen is empty.
     *
     * @throws DataAccessException if the count query or the seed inserts fail
     */
    public static void seedIfEmpty() {
        try (Connection connection = getConnection()) {
            int count = 0;
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM pathogen")) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
            if (count == 0) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(SEED_PATHOGENS_SQL);
                    statement.execute(SEED_DISTRICTS_SQL);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to seed reference data", e);
        }
    }

    /**
     * @return {@code true} if SQLite's {@code PRAGMA integrity_check} reports {@code ok}
     * @throws DataAccessException if the check itself could not be run
     */
    public static boolean isHealthy() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA integrity_check")) {
            return rs.next() && "ok".equalsIgnoreCase(rs.getString(1));
        } catch (SQLException e) {
            throw new DataAccessException("Database integrity check could not be run", e);
        }
    }

    /**
     * Closes any connection this manager is tracking (the most recently opened one, and/or the
     * in-memory test keep-alive connection) and, if the latter was open, resets the URL back to the
     * real {@code data/episim.db} file.
     *
     * @throws DataAccessException if closing a tracked connection fails
     */
    public static void close() {
        if (lastOpenedConnection != null) {
            try {
                if (!lastOpenedConnection.isClosed()) {
                    lastOpenedConnection.close();
                }
            } catch (SQLException e) {
                throw new DataAccessException("Failed to close the database connection", e);
            } finally {
                lastOpenedConnection = null;
            }
        }
        if (inMemoryKeepAliveConnection != null) {
            try {
                inMemoryKeepAliveConnection.close();
            } catch (SQLException e) {
                throw new DataAccessException("Failed to close the in-memory test database", e);
            } finally {
                inMemoryKeepAliveConnection = null;
                dbUrl = DEFAULT_DB_URL;
            }
        }
    }

    /** Reads {@code /schema.sql} off the classpath in full, as UTF-8 text. */
    private static String readSchemaResource() {
        try (InputStream in = DatabaseManager.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql not found on classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Unable to read schema.sql from the classpath", e);
        }
    }

    /** Splits a SQL script on ';', ignoring semicolons inside string literals and skipping "--" comment lines. */
    private static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;

        for (String line : script.split("\n", -1)) {
            String trimmed = line.trim();
            if (!inSingleQuote && trimmed.startsWith("--")) {
                continue;
            }
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '\'') {
                    inSingleQuote = !inSingleQuote;
                    current.append(c);
                } else if (c == ';' && !inSingleQuote) {
                    statements.add(current.toString().trim());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            current.append('\n');
        }
        if (!current.toString().trim().isEmpty()) {
            statements.add(current.toString().trim());
        }
        return statements;
    }

    private static final String SEED_PATHOGENS_SQL = """
            INSERT OR IGNORE INTO pathogen
                (name, r0, incubation_days, infectious_days, hospitalisation_rate,
                 mortality_rate, vaccine_effectiveness, description)
            VALUES
                ('COVID-19 (Delta-like)', 5.1, 4, 10, 0.055, 0.018, 0.85,
                 'High transmissibility respiratory virus with significant hospitalisation burden.'),
                ('Seasonal Influenza',    1.4, 2,  6, 0.012, 0.001, 0.60,
                 'Endemic respiratory illness; low mortality but high annual case volume.'),
                ('Measles',               14.0, 11, 8, 0.200, 0.002, 0.97,
                 'Extremely transmissible; vaccine-preventable. Illustrates herd-immunity thresholds.'),
                ('Novel Pathogen X',      3.0, 6, 12, 0.090, 0.045, 0.40,
                 'Hypothetical emerging pathogen used for preparedness planning (SDG Target 3.d).')
            """;

    private static final String SEED_DISTRICTS_SQL = """
            INSERT OR IGNORE INTO district
                (district_id, name, population, density_factor, hospital_capacity)
            VALUES
                ('KL-CENTRAL', 'Kuala Lumpur City Centre', 4000, 1.65,  90),
                ('PJ-URBAN',   'Petaling Jaya',            3000, 1.25,  70),
                ('SHAH-SUB',   'Shah Alam Suburbs',        2000, 0.95,  45),
                ('RURAL-N',    'Northern Rural Zone',      1000, 0.55,  15)
            """;
}
