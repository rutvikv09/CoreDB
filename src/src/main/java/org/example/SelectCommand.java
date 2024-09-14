package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SelectCommand {

    /**
     * Executes the SELECT command to retrieve records from a table.
     *
     * @param tokens The tokens parsed from the command input.
     * @param input The complete command input string.
     * @throws Exception If an error occurs during command execution.
     */
    public static void execute(String[] tokens, String input) throws Exception {
        if (CommandProcessor.activeDatabase == null) {
            throw new Exception("No database selected.");
        }

        // Add logging before executing the select query
        LogManager.logQuery("SELECT command initiated", "Attempting to execute SELECT query");

        if (input.endsWith(";")) {
            input = input.substring(0, input.length() - 1).trim();
        }

        // Check if the last token ends with a semicolon
        if (tokens.length > 0 && tokens[tokens.length - 1].endsWith(";")) {
            // Remove the semicolon from the last token
            tokens[tokens.length - 1] = tokens[tokens.length - 1].substring(0, tokens[tokens.length - 1].length() - 1);
        }

        // Extract columns and table name from the query
        String columnsPart = input.substring(7, input.indexOf("FROM")).trim();
        String tableName = tokens[3].trim(); // Extract table name

        // Check if the table file exists
        File tableFile = new File("tinydb/databases/" + CommandProcessor.activeDatabase + "/" + tableName + ".txt");
        if (!tableFile.exists()) {
            throw new Exception("Table does not exist.");
        }

        String[] tableColumns = null;
        List<Map<String, String>> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
            // Read the first line to get column names
            String firstLine = reader.readLine();
            if (firstLine != null && !firstLine.trim().isEmpty()) {
                tableColumns = firstLine.split(",");
            }

            // Process each subsequent line (data rows)
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                String[] rowValues = line.split(",");

                // Create a map to store values for this row
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < tableColumns.length; i++) {
                    String columnName = tableColumns[i].trim();
                    if (i < rowValues.length) {
                        String value = rowValues[i].trim().replaceAll("^'|'$", ""); // Remove surrounding single quotes
                        rowMap.put(columnName, value);
                    } else {
                        rowMap.put(columnName, ""); // Handle case where row has fewer columns
                    }
                }
                rows.add(rowMap);
            }
        } catch (IOException e) {
            System.out.println("Error reading table file: " + e.getMessage());
            return;
        }

        // Parse the WHERE condition if it exists
        String condition = null;
        if (input.contains("WHERE")) {
            condition = input.substring(input.indexOf("WHERE") + 5).trim(); // Extract condition part

            // Split condition into column name, operator, and value
            String[] conditionParts = condition.split(" ");
            if (conditionParts.length != 3) {
                throw new Exception("Invalid condition format.");
            }
            String columnName = conditionParts[0].trim();
            String operator = conditionParts[1].trim();
            String value = conditionParts[2].trim().replaceAll("^'|'$", ""); // Remove surrounding single quotes

            // Validate if the column exists in the table
            boolean columnExists = false;
            for (String col : tableColumns) {
                String columnNameFromFile = col.trim().split("\\s+")[0]; // Get the column name part
                if (columnNameFromFile.equalsIgnoreCase(columnName)) {
                    columnExists = true;
                    break;
                }
            }

            if (!columnExists) {
                throw new Exception("Column '" + columnName + "' does not exist in table '" + tableName + "'.");
            }

            // Filter rows based on the condition
            List<Map<String, String>> filteredRows = new ArrayList<>();
            for (Map<String, String> row : rows) {
                String columnValue = row.get(columnName);
                if (columnValue != null && compareValues(columnValue, operator, value)) {
                    filteredRows.add(row);
                }
            }

            rows = filteredRows; // Update rows to filteredRows
        }

        // Extract and validate the selected columns
        String[] selectedColumns;
        if (columnsPart.equals("*")) {
            selectedColumns = tableColumns; // Select all columns
        } else {
            selectedColumns = columnsPart.split(",");
            for (int i = 0; i < selectedColumns.length; i++) {
                selectedColumns[i] = selectedColumns[i].trim();
            }

            List<String> validColumns = new ArrayList<>();
            for (String selectedColumn : selectedColumns) {
                boolean columnExists = false;
                for (String tableColumn : tableColumns) {
                    if (tableColumn.trim().equalsIgnoreCase(selectedColumn)) {
                        validColumns.add(tableColumn.trim());
                        columnExists = true;
                        break;
                    }
                }
                if (!columnExists) {
                    throw new Exception("Column '" + selectedColumn + "' does not exist in table '" + tableName + "'.");
                }
            }
            selectedColumns = validColumns.toArray(new String[0]);
        }

        // Print the selected rows and columns
        if (!rows.isEmpty()) {
            printRows(rows, selectedColumns);
            LogManager.logQuery("SELECT command executed", "Records retrieved successfully from table: " + tableName);
        } else {
            System.out.println("No matching records found.");
            LogManager.logQuery("SELECT command executed", "No matching records found for the query on table: " + tableName);
        }
    }

    /**
     * Utility method to compare values based on the operator.
     *
     * @param columnValue The value in the column.
     * @param operator The operator to use for comparison.
     * @param value The target value to compare against.
     * @return true if the comparison is successful, false otherwise.
     * @throws Exception If an error occurs during comparison.
     */
    private static boolean compareValues(String columnValue, String operator, String value) throws Exception {
        try {
            double columnNumericValue = Double.parseDouble(columnValue);
            double targetValue = Double.parseDouble(value);

            switch (operator) {
                case ">":
                    return columnNumericValue > targetValue;
                case ">=":
                    return columnNumericValue >= targetValue;
                case "<":
                    return columnNumericValue < targetValue;
                case "<=":
                    return columnNumericValue <= targetValue;
                case "=":
                    return columnNumericValue == targetValue;
                default:
                    throw new Exception("Unsupported operator: " + operator);
            }
        } catch (NumberFormatException e) {
            throw new Exception("Comparison value must be numeric.");
        }
    }

    /**
     * Utility method to print the selected rows and columns.
     *
     * @param rows The list of rows to print.
     * @param columns The list of columns to print.
     */
    private static void printRows(List<Map<String, String>> rows, String[] columns) {
        for (String column : columns) {
            System.out.print(column + "\t");
        }
        System.out.println();
        for (Map<String, String> row : rows) {
            for (String column : columns) {
                System.out.print(row.get(column) + "\t");
            }
            System.out.println();
        }
    }
}
