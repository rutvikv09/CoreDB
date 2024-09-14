package org.example;

import java.io.*;

public class DeleteCommand {

    /**
     * Executes the DELETE command to delete records from a table based on the specified condition.
     *
     * @param tokens The tokens parsed from the command input.
     * @throws Exception If an error occurs during command execution.
     */
    public static void execute(String[] tokens) throws Exception {
        if (CommandProcessor.activeDatabase == null) {
            throw new Exception("No database selected.");
        }

        // Add logging before deleting
        LogManager.logGeneral("DELETE command initiated", "Attempting to delete record");

        // Remove semicolon at the end of the input if it exists
        if (tokens.length > 0 && tokens[tokens.length - 1].endsWith(";")) {
            tokens[tokens.length - 1] = tokens[tokens.length - 1].substring(0, tokens[tokens.length - 1].length() - 1);
        }

        // Extract table name
        if (tokens.length < 4 || !tokens[3].equalsIgnoreCase("WHERE")) {
            throw new Exception("Invalid delete statement format: Missing WHERE clause.");
        }
        String tableName = tokens[2];

        // Extract condition from tokens
        StringBuilder conditionBuilder = new StringBuilder();
        for (int i = 4; i < tokens.length; i++) {
            conditionBuilder.append(tokens[i]).append(" ");
        }
        String condition = conditionBuilder.toString().trim();

        // Split condition into column name and value
        String[] conditionParts = condition.split("=");
        if (conditionParts.length != 2) {
            throw new Exception("Invalid condition format: " + condition);
        }
        String columnName = conditionParts[0].trim();
        String conditionValueWithQuotes = conditionParts[1].trim(); // Extract value (with or without surrounding quotes)

        // Remove surrounding single quotes from condition value if they exist
        String conditionValue = conditionValueWithQuotes.replaceAll("^['\"]|['\"]$", "");

        // Check if the table file exists
        File tableFile = new File("tinydb/databases/" + CommandProcessor.activeDatabase + "/" + tableName + ".txt");
        if (!tableFile.exists()) {
            throw new Exception("Table does not exist.");
        }

        File tempFile = new File("tinydb/databases/" + CommandProcessor.activeDatabase + "/" + tableName + "_temp.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            boolean deleted = false;
            String[] headers = null;
            while ((line = reader.readLine()) != null) {
                if (headers == null) {
                    headers = line.split(","); // Read headers if not read yet
                    writer.write(line); // Write header to temp file
                    writer.newLine();
                    continue;
                }

                // Split the line into columns
                String[] columns = line.split(",");

                // Check if the line contains the condition to be deleted
                boolean matchesCondition = checkCondition(columns, headers, columnName, conditionValue);
                if (!matchesCondition) {
                    writer.write(line);
                    writer.newLine();
                } else {
                    deleted = true; // Mark as deleted if at least one line matches condition
                    LogManager.logGeneral("DELETE command executed", "Record deleted from table: " + tableName);
                }
            }

            if (!deleted) {
                System.out.println("No matching records found for the delete condition.");
                tempFile.delete(); // Delete temp file if no delete was performed
                return;
            }
        } catch (IOException e) {
            System.out.println("Error processing table file: " + e.getMessage());
            return;
        }

        // Replace the original table file with the updated temp file
        if (!tableFile.delete()) {
            throw new Exception("Failed to delete the original table file.");
        }
        if (!tempFile.renameTo(tableFile)) {
            throw new Exception("Failed to rename the temporary table file.");
        }

        System.out.println("Record deleted successfully.");
    }

    /**
     * Checks if the condition matches the values in the columns.
     *
     * @param columns The array of column values.
     * @param headers The array of header names.
     * @param columnName The name of the column to check.
     * @param conditionValue The value to check against.
     * @return true if the condition matches, false otherwise.
     */
    private static boolean checkCondition(String[] columns, String[] headers, String columnName, String conditionValue) {
        // Find the index of the column in headers
        int columnIndex = getColumnIndex(headers, columnName);
        if (columnIndex == -1) {
            System.out.println("Column '" + columnName + "' not found in table headers.");
            return false;
        }

        // Check if the value in the column matches the condition value
        String columnValue = columns[columnIndex].trim();

        // Check both quoted and unquoted values
        return columnValue.equalsIgnoreCase(conditionValue) || columnValue.equalsIgnoreCase("'" + conditionValue + "'");
    }

    /**
     * Retrieves the index of the column in the headers array.
     *
     * @param headers The array of header names.
     * @param columnName The name of the column.
     * @return The index of the column in the headers array, or -1 if not found.
     */
    private static int getColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1; // Return -1 if column not found
    }
}
