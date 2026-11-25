package com.legacybank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the schema and loads the seed data when the SQLite database file is empty.
 * The scripts are the same files found in the /database folder (they are also packaged
 * into the jar under /database).
 */
public class DatabaseInitializer {

    public static final String SCHEMA_SCRIPT = "/database/sqlite-schema.sql";
    public static final String SEED_SCRIPT = "/database/seed-data.sql";

    /** Initialises the database if the EMPLOYEES table does not exist yet. */
    public static void initializeIfNeeded() throws SQLException, IOException {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'EMPLOYEES'");
            rs.next();
            boolean exists = rs.getInt(1) > 0;
            DBConnection.close(rs, st);
            if (exists) {
                System.out.println("INFO: database already initialised");
                return;
            }
            System.out.println("INFO: empty database, creating schema and loading seed data");
            runScript(conn, SCHEMA_SCRIPT);
            runScript(conn, SEED_SCRIPT);
            System.out.println("INFO: database initialised");
        } finally {
            DBConnection.close(rs, st, conn);
        }
    }

    /** Runs a classpath SQL script inside one transaction. */
    public static void runScript(Connection conn, String resource) throws SQLException, IOException {
        List<String> statements = readStatements(resource);
        boolean oldAutoCommit = conn.getAutoCommit();
        Statement st = null;
        try {
            conn.setAutoCommit(false);
            st = conn.createStatement();
            for (int i = 0; i < statements.size(); i++) {
                st.execute(statements.get(i));
            }
            conn.commit();
        } catch (SQLException e) {
            DBConnection.rollback(conn);
            throw e;
        } finally {
            DBConnection.close(st);
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    /** Reads a script and splits it into statements: '--' comment lines are dropped, ';' ends a statement. */
    static List<String> readStatements(String resource) throws IOException {
        InputStream in = DatabaseInitializer.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("SQL script not found on classpath: " + resource);
        }
        List<String> result = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuffer current = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.length() == 0 || trimmed.startsWith("--")) {
                    continue;
                }
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    String sql = current.toString().trim();
                    result.add(sql.substring(0, sql.length() - 1));
                    current.setLength(0);
                }
            }
            if (current.toString().trim().length() > 0) {
                result.add(current.toString().trim());
            }
        } finally {
            DBConnection.close(reader);
        }
        return result;
    }
}
