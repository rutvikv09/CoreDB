package org.example;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InsertCommand {

    /**
     * Executes the INSERT command to insert a record into a table.
     *
     * @param tokens The tokens parsed from the command input.
     * @param input The complete command input string.
     * @throws Exception If an error occurs during command execution.
     */
    public static void execute(String[] tokens, String input) throws Exception {
        if (CommandProcessor.activeDatabase == null) {
            throw new Exception("No database selected.");
        }

        // Add logging before inserting the record
        LogManager.logQuery("INSERT command initiated", "Attempting to insert record");

        // Extract table name
        String tableName = extractTableName(input);

        // Extract column names and values
        String[] parts = input.split("VALUES", 2);
        if (parts.length != 2) {
            throw new Exception("Invalid INSERT statement format: " + input);
        }
        String columnNamesWithParentheses = parts[0].trim();
        String valuesPart = parts[1].trim();

        // Remove "INSERT INTO tablename" part to get just "(col1, col2, ...)"
        String columnNames = extractColumnNames(columnNamesWithParentheses);

        // Extract values and validate primary key
        String values = extractValues(valuesPart);
        validatePrimaryKey(tableName, columnNames, values);

        // Construct file path with sanitized table name
        File tableFile = new File("tinydb/databases/" + CommandProcessor.activeDatabase + "/" + tableName + ".txt");
        if (tableFile.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, true))) {
                writer.newLine();
                writer.write(values);
                System.out.println("Record inserted successfully into table " + tableName + ".");
                LogManager.logQuery("INSERT command executed", "Record inserted successfully into table: " + tableName);
            } catch (IOException e) {
                throw new Exception("Error writing to table file: " + e.getMessage());
            }
        } else {
            throw new Exception("Table file path: " + tableFile.getAbsolutePath() + " does not exist.");
        }
    }

    /**
     * Extracts the table name from the INSERT command.
     *
     * @param input The complete command input string.
     * @return The table name.
     * @throws Exception If the table name cannot be extracted.
     */
    private static String extractTableName(String input) throws Exception {
        // Extract table name after "INSERT INTO"
        String regex = "(?i)INSERT\\s+INTO\\s+(\\w+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        StringBuilder res = new StringBuilder();
        while (matcher.find()) {
            res.append(matcher.group(1));
        }
        return res.toString().trim();
    }

    /**
     * Extracts the column names from the INSERT command.
     *
     * @param columnNamesWithParentheses The part of the command containing column names with parentheses.
     * @return The column names.
     */
    private static String extractColumnNames(String columnNamesWithParentheses) {
        // Remove "INSERT INTO tablename" part to get just "(col1, col2, ...)"
        int startIndex = columnNamesWithParentheses.indexOf("(");
        int endIndex = columnNamesWithParentheses.indexOf(")");
        return columnNamesWithParentheses.substring(startIndex + 1, endIndex);
    }

    /**
     * Extracts the values from the INSERT command.
     *
     * @param valuesPart The part of the command containing values.
     * @return The extracted values.
     */
    private static String extractValues(String valuesPart) {
        // Remove surrounding parentheses and trim whitespace
        valuesPart = valuesPart.trim();
        if (valuesPart.startsWith("(") && valuesPart.endsWith(")")) {
            valuesPart = valuesPart.substring(1, valuesPart.length() - 1);
        }

        // Remove surrounding single quotes for string values
        valuesPart = valuesPart.replaceAll("^'|'$", "");

        return valuesPart;
    }

    /**
     * Validates the primary key for uniqueness before inserting the record.
     *
     * @param tableName The name of the table.
     * @param columnNames The column names.
     * @param values The values to insert.
     * @throws Exception If the primary key is not unique or an error occurs.
     */
    private static void validatePrimaryKey(String tableName, String columnNames, String values) throws Exception {
        // Check if the table metadata file exists
        File metaFile = new File("tinydb/databases/" + CommandProcessor.activeDatabase + "/" + tableName + "_meta.txt");
        if (!metaFile.exists()) {
            throw new Exception("Metadata file for table " + tableName + " does not exist.");
        }

        // Get the primary key column name from metadata
        String primaryKeyColumnName = extractPrimaryKeyColumnName(metaFile);

        // Split column names and values by comma
        String[] columns = columnNames.split(",");
        String[] columnValues = values.split(",");

        // Find index of primary key column
        int primaryKeyIndex = -1;
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].trim().equalsIgnoreCase(primaryKeyColumnName)) {
                primaryKeyIndex = i;
                break;
            }
        }

        if (primaryKeyIndex == -1) {
            throw new Exception("Primary key column not found in the INSERT statement for table " + tableName);
        }

        // Validate primary key uniqueness
        validatePrimaryKeyUniqueness(tableName, primaryKeyIndex, columnValues);
    }

    /**
     * Extracts the primary key column name from the metadata file.
     *
     * @param metaFile The metadata file.
     * @return The primary key column name.
     * @throws IOException If an I/O error occurs.
     */
    private static String extractPrimaryKeyColumnName(File metaFile) throws IOException {
        String primaryKeyColumnName = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
            String line;

            // Read table structure from metadata file
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Primary Key: ")) {
                    primaryKeyColumnName = line.substring("Primary Key: ".length()).trim();
                    break;
                }
            }
        }

        if (primaryKeyColumnName == null) {
            throw new IOException("Metadata file does not contain primary key information.");
        }

        return primaryKeyColumnName;
    }

    /**
     * Validates the uniqueness of the primary key before inserting the record.
     *
     * @param tableName The name of the table.
     * @param primaryKeyIndex The index of the primary key column.
     * @param columnValues The values to insert.
     * @throws Exception If the primary key is not unique or an error occurs.
     */
    private static void validatePrimaryKeyUniqueness(String tableName, int primaryKeyIndex, String[] columnValues) throws Exception {
        // Construct file path with sanitized table name
        File tableFile = new File("tinydb/databases/" + CommandProcessor.activeDatabase + "/" + tableName + ".txt");

        if (tableFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
                String line;

                // Read existing data from table file and validate primary key uniqueness
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");
                    if (values.length > primaryKeyIndex) {
                        if (values[primaryKeyIndex].trim().equals(columnValues[primaryKeyIndex].trim())) {
                            throw new Exception("Primary key value is not unique in table " + tableName);
                        }
                    }
                }
            }
        }
    }
}
