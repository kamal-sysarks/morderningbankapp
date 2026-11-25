package com.legacybank;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point of the Legacy Banking Transaction Management System.
 */
public class BankingApplication {

    public static void main(String[] args) {
        System.out.println("Legacy Banking System starting...");
        System.out.println("Database URL: " + DBConnection.getUrl());
        try {
            DatabaseInitializer.initializeIfNeeded();
        } catch (Exception e) {
            System.out.println("ERROR: database initialisation failed: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Could not initialise the database:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("WARN: could not set look and feel: " + e.getMessage());
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new LoginFrame().setVisible(true);
            }
        });
    }
}
