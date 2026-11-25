package com.legacybank;

/**
 * Global session state (classic legacy style: a static holder for the logged in employee).
 */
public class AppSession {

    public static long employeeId;
    public static String username;
    public static String employeeName;

    public static void clear() {
        employeeId = 0;
        username = null;
        employeeName = null;
    }
}
