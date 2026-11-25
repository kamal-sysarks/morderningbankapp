package com.legacybank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central place that creates JDBC connections to the SQLite database.
 *
 * Connection settings are read (lowest to highest priority) from:
 *   1. db.properties on the classpath (src/main/resources/db.properties)
 *   2. an external file named by the -Ddb.config=/path/file.properties system property
 *   3. -Ddb.url, -Ddb.username, -Ddb.password, -Ddb.driver system properties
 */
public class DBConnection {

    private static Properties props = null;

    private static synchronized Properties loadProperties() {
        if (props != null) {
            return props;
        }
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = DBConnection.class.getResourceAsStream("/db.properties");
            if (in != null) {
                p.load(in);
            } else {
                System.out.println("WARN: db.properties not found on classpath");
            }
        } catch (IOException e) {
            System.out.println("ERROR: could not read db.properties: " + e.getMessage());
        } finally {
            closeQuietly(in);
        }

        String external = System.getProperty("db.config");
        if (external != null && external.length() > 0) {
            InputStream ext = null;
            try {
                ext = new FileInputStream(new File(external));
                p.load(ext);
                System.out.println("INFO: loaded database configuration from " + external);
            } catch (IOException e) {
                System.out.println("ERROR: could not read " + external + ": " + e.getMessage());
            } finally {
                closeQuietly(ext);
            }
        }

        String[] keys = { "db.driver", "db.url", "db.username", "db.password" };
        for (int i = 0; i < keys.length; i++) {
            String override = System.getProperty(keys[i]);
            if (override != null) {
                p.setProperty(keys[i], override);
            }
        }
        props = p;
        return props;
    }

    /** Forces the configuration to be re-read (used by tests that switch databases). */
    public static synchronized void reset() {
        props = null;
    }

    private static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static String getUrl() {
        return loadProperties().getProperty("db.url");
    }

    public static Connection getConnection() throws SQLException {
        Properties p = loadProperties();
        String driver = p.getProperty("db.driver", "org.sqlite.JDBC");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found: " + driver);
        }

        // SQLite specific connection settings:
        //  foreign_keys     - enforce foreign keys (off by default in SQLite)
        //  busy_timeout     - wait up to 10s for another window's write lock instead of failing
        //  transaction_mode - IMMEDIATE takes the write lock when a transaction starts, so the
        //                     balance check and the balance update of a withdrawal/transfer
        //                     cannot interleave with another teller's update.
        Properties connProps = new Properties();
        connProps.setProperty("foreign_keys", "true");
        connProps.setProperty("busy_timeout", "10000");
        connProps.setProperty("transaction_mode", "IMMEDIATE");
        String user = p.getProperty("db.username");
        if (user != null && user.length() > 0) {
            connProps.setProperty("user", user);
            connProps.setProperty("password", p.getProperty("db.password", ""));
        }
        return DriverManager.getConnection(p.getProperty("db.url"), connProps);
    }

    /** Closes any JDBC resource without throwing. */
    public static void close(AutoCloseable... resources) {
        for (int i = 0; i < resources.length; i++) {
            if (resources[i] != null) {
                try {
                    resources[i].close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    public static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
                System.out.println("WARN: transaction rolled back");
            } catch (SQLException e) {
                System.out.println("ERROR: rollback failed: " + e.getMessage());
            }
        }
    }
}
