package com.app.registre;

public class Session {
    private static String currentUser;
    private static String displayName;

    public static void setCurrentUser(String username) { currentUser = username; }
    public static String getCurrentUser() { return currentUser; }

    public static void setDisplayName(String name) { displayName = name; }
    public static String getDisplayName() { return displayName; }

    public static void clear() { currentUser = null; displayName = null; }
}
