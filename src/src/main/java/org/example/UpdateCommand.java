package org.example;

import java.io.*;

public class UpdateCommand {

    /**
     * Executes the UPDATE command to modify records in a table.
     *
     * @param input The complete command input string.
     * @throws Exception If an error occurs during command execution.
     */
    public static void execute(String input) throws Exception {
        if (CommandProcessor.activeDatabase == null) {
            throw new Exception("No database selected.");
        }

        // Add logging before updating the record
        LogManager.logQuery("UPDATE command initiated", "Attempting to update record");

        if (input.endsWith(";")) {
            input = input.substring(0, input.length() - 1).trim();
        }

        // Extract table name, SET clause, and WHERE condition from the query
        String[] tokens = input.split("\\s+");
        String tableName = tokens[1].trim(); // Extract table name

        // Extract SET clause
        int setIndex = input.indexOf("SET");
        int whereIndex = input.indexOf("WHERE");
        if (setIndex == -1 || whereIndex == -1 || whereIndex < setIndex) {
            throw new Exception("Invalid update query format.");
        }
        String setClause = input.substring(setIndex + 3, whereIndex).trim();

        // Extract WHERE condition
        String condition = input.substring(whereIndex + 5).trim();

        // Split SET and WHERE conditions into column and value parts
        String[] setParts = setClause.split("=");
        String[] conditionParts = condition.split("=");

        if (setParts.length != 2 || conditionParts.length != 2) {
            throw new Exception("Invalid SET or WHERE clause format.");
        }

        String setColumn = setParts[0].trim();
        String setValue = setParts[1].trim().replaceAll("'", "");
        String conditionColumn = conditionParts[0].trim();
        String conditionValue = conditionParts[1].replaceAll("'", "").trim(); // Remove surrounding single quotes

        // Check if the table file exists
        File tableFile = new File("tinydb/databases/" + CommandProcessor.activeDatabase + "/" + tableName + ".txt");
        if (!tableFile.exists()) {
            throw new Exception("Table file does not exist for table: " + tableName);
        }

        // Validate primary key update
        validatePrimaryKeyUpdate(tableName, setColumn);

        File tempFile = new File("tinydb/databases/" + CommandProcessor.activeDatabase + "/" + tableName + "_temp.txt");

        boolean updated = false; // Flag to check if any record was updated

        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String header = reader.readLine(); // Read header line
            if (header != null) {
                writer.write(header); // Write header to temp file
                writer.newLine();
            }

            String[] headers = header.split(",");
            int conditionColumnIndex = getColumnIndex(headers, conditionColumn);

            if (conditionColumnIndex == -1) {
                throw new Exception("Invalid column name in WHERE clause.");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) continue;

                // Split the line into columns
                String[] columns = line.split(",");

                // Check if the line matches the WHERE condition
                if (columns[conditionColumnIndex].trim().equals(conditionValue)) {
                    int setColumnIndex = getColumnIndex(headers, setColumn);
                    if (setColumnIndex != -1) {
                        // Validate primary key update before updating
                        validatePrimaryKeyUpdate(tableName, headers[setColumnIndex].trim());

                        columns[setColumnIndex] = setValue;
                        updated = true; // Mark as updated
                    } else {
                        throw new Exception("Invalid column name in SET clause.");
                    }
                }

                // Write the updated line (or original if not updated) to the temp file
                writer.write(String.join(",", columns));
                writer.newLine(); // Move to next line
            }
        } catch (IOException e) {
            System.out.println("Error processing table file: " + e.getMessage());
            return;
        }

        // Check if any record was updated
        if (!updated) {
            System.out.println("No matching records found for the update condition.");
            tempFile.delete(); // Delete temp file if no update was performed
            LogManager.logQuery("UPDATE command executed", "No matching records found for the update condition on table: " + tableName);
            return;
        }

        // Replace the original table file with the updated temp file
        if (!tableFile.delete()) {
            throw new Exception("Failed to delete the original table file.");
        }
        if (!tempFile.renameTo(tableFile)) {
            throw new Exception("Failed to rename the temporary table file.");
        }

        System.out.println("Record updated successfully.");
        LogManager.logQuery("UPDATE command executed", "Record updated successfully in table: " + tableName);
    }

    /**
     * Gets the index of a column in the table header.
     *
     * @param headers The table headers.
     * @param columnName The column name to find.
     * @return The index of the column, or -1 if not found.
     */
    private static int getColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1; // Return -1 if column not found
    }

    /**
     * Validates if the primary key is being updated.
     *
     * @param tableName The table name.
     * @param columnName The column name to check.
     * @throws Exception If an error occurs during validation or if the primary key is being updated.
     */
    private static void validatePrimaryKeyUpdate(String tableName, String columnName) throws Exception {
        // Check if the table metadata file exists
        File metaFile = new File("tinydb/databases/" + CommandProcessor.activeDatabase + "/" + tableName + "_meta.txt");
        if (!metaFile.exists()) {
            throw new Exception("Metadata file for table " + tableName + " does not exist.");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
            String line;

            // Read table structure from metadata file
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("Primary Key: ")) {
                    String primaryKeyColumnName = line.substring("Primary Key: ".length()).trim();
                    if (columnName.equalsIgnoreCase(primaryKeyColumnName)) {
                        throw new Exception("Updating primary key column " + columnName + " is not allowed.");
                    }
                    break;
                }
            }
        }
    }
}
