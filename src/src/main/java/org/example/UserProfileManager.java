package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class UserProfileManager {

    private static final String FILE_NAME = "User_Profile.txt";

    /**
     * Hashes a given string using MD5.
     *
     * @param input The input string to be hashed.
     * @return The MD5 hash of the input string.
     */
    public static String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads user profiles from a file.
     *
     * @return A map of user profiles where the key is the username and the value is an array containing the hashed password, email, and role.
     */
    public static Map<String, String[]> loadUserProfiles() {
        Map<String, String[]> userProfiles = new HashMap<>();
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String[] parts = scanner.nextLine().split(",");
                    if (parts.length == 4) {
                        userProfiles.put(parts[0], new String[]{parts[1], parts[2], parts[3]});
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading user profiles: " + e.getMessage());
            }
        }
        return userProfiles;
    }

    /**
     * Saves user profiles to a file.
     *
     * @param userProfiles A map of user profiles where the key is the username and the value is an array containing the hashed password, email, and role.
     */
    public static void saveUserProfiles(Map<String, String[]> userProfiles) {
        try (FileWriter writer = new FileWriter(FILE_NAME)) {
            for (Map.Entry<String, String[]> entry : userProfiles.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue()[0] + "," + entry.getValue()[1] + "," + entry.getValue()[2] + "\n");
            }
        } catch (IOException e) {
            System.out.println("Error saving user profiles: " + e.getMessage());
        }
    }

    /**
     * Logs user activity.
     *
     * @param activityType The type of activity being logged (e.g., LOGIN, LOGOUT).
     * @param username The username of the user performing the activity.
     */
    public static void logUserActivity(String activityType, String username) {
        LogManager.logUserActivity(activityType, username);
    }
}
