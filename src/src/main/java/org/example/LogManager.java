package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class LogManager {

    /**
     * Logs a general message with a specific state to the general log file.
     * @param message The general message to log.
     * @param state The state associated with the message.
     */
    public static void logGeneral(String message, String state) {
        writeLog("general_log.json", createLogEntry(message, state));
    }

    /**
     * Logs an event with a specific type and description to the event log file.
     * @param eventType The type of the event.
     * @param description A description of the event.
     */
    public static void logEvent(String eventType, String description) {
        writeLog("event_log.json", createLogEntry(eventType, description));
    }

    /**
     * Logs a query with a specific type and detail to the query log file.
     * @param query The query type.
     * @param attemptingToInsertRecord Details of the query.
     */
    public static void logQuery(String query, String attemptingToInsertRecord) {
        writeLog("query_log.json", createLogEntry("Query", query));
    }

    /**
     * Logs a transaction with a specific type and detail to the transaction log file.
     * @param transactionType The type of the transaction.
     * @param transactionDetail Details of the transaction.
     */
    public static void logTransaction(String transactionType, String transactionDetail) {
        writeLog("transaction_log.json", createLogEntry(transactionType, transactionDetail));
    }

    /**
     * Logs user activity with a specific type and username to the user activity log file.
     * @param activityType The type of user activity.
     * @param username The username associated with the activity.
     */
    public static void logUserActivity(String activityType, String username) {
        writeLog("user_activity_log.json", createLogEntry(activityType, username));
    }

    /**
     * Writes a log entry to the specified log file.
     * @param logFileName The name of the log file.
     * @param logEntry The log entry to write.
     */
    private static void writeLog(String logFileName, String logEntry) {
        String logFilePath = "logs/" + logFileName;
        createLogDirectoryIfNotExists("logs");

        try (FileWriter fileWriter = new FileWriter(logFilePath, true)) {
            fileWriter.write(logEntry + "\n");
        } catch (IOException e) {
            System.out.println("Error writing log: " + e.getMessage());
        }
    }

    /**
     * Creates the log directory if it does not already exist.
     * @param logDirectoryPath The path to the log directory.
     */
    private static void createLogDirectoryIfNotExists(String logDirectoryPath) {
        File logDirectory = new File(logDirectoryPath);
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }
    }

    /**
     * Creates a log entry with the given key and value.
     * @param key The key for the log entry.
     * @param value The value for the log entry.
     * @return A JSON-formatted string representing the log entry.
     */
    private static String createLogEntry(String key, String value) {
        Map<String, String> logEntry = new HashMap<>();
        logEntry.put("timestamp", LocalDateTime.now().toString());
        logEntry.put(key, value);

        return toJson(logEntry);
    }

    /**
     * Converts a map to a JSON-formatted string.
     * @param map The map to convert.
     * @return A JSON-formatted string representing the map.
     */
    private static String toJson(Map<String, String> map) {
        StringBuilder jsonBuilder = new StringBuilder("{");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            jsonBuilder.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\", ");
        }
        jsonBuilder.delete(jsonBuilder.length() - 2, jsonBuilder.length()); // Remove trailing comma and space
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }
}
