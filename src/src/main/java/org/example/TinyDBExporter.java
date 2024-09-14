package org.example;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TinyDBExporter {

    /**
     * Retrieves column names, data types, and primary key info from the metadata file.
     *
     * @param metaFilePath The path to the metadata file.
     * @return A map containing column names and their data types.
     * @throws IOException If an I/O error occurs while reading the metadata file.
     */
    public static Map<String, String> getColumnNamesAndTypes(String metaFilePath) throws IOException {
        Path metaPath = Paths.get(metaFilePath);
        Path tempMetaPath = Paths.get(metaFilePath.replace("_meta.txt", "_temp_meta.txt"));

        // Check if the metadata file exists, if not check for the temp metadata file
        if (!Files.exists(metaPath)) {
            if (Files.exists(tempMetaPath)) {
                metaPath = tempMetaPath;
            } else {
                throw new IOException("Metadata file not found: " + metaFilePath);
            }
        }

        Map<String, String> columnDetails = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(metaPath);
        if (lines.size() > 1) {
            String[] columns = lines.get(1).split(":")[1].split(",");
            for (String column : columns) {
                String[] parts = column.trim().split(" ");
                // Handle primary key
                if (parts[1].equals("(pk)")) {
                    columnDetails.put(parts[0].toLowerCase(), "INT PRIMARY KEY"); // Use lowercase for consistency
                } else {
                    columnDetails.put(parts[0].toLowerCase(), parts[1]);
                }
            }
        }
        return columnDetails;
    }

    /**
     * Retrieves all data from the table file, excluding the first line (column names).
     *
     * @param tableFilePath The path to the table file.
     * @return A list of string arrays, each representing a row in the table.
     * @throws IOException If an I/O error occurs while reading the table file.
     */
    public static List<String[]> getTableData(String tableFilePath) throws IOException {
        List<String[]> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(tableFilePath))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    // Skip the first line containing column names
                    firstLine = false;
                    continue;
                }
                String[] row = line.split(",");
                if (row.length > 0 && !row[0].trim().isEmpty()) {
                    data.add(row);
                }
            }
        }
        return data;
    }

    /**
     * Retrieves foreign key relationships from the metadata file.
     *
     * @param metaFilePath The path to the metadata file.
     * @return A map containing foreign key relationships.
     * @throws IOException If an I/O error occurs while reading the metadata file.
     */
    public static Map<String, String> getForeignKeys(String metaFilePath) throws IOException {
        Path metaPath = Paths.get(metaFilePath);
        Path tempMetaPath = Paths.get(metaFilePath.replace("_meta.txt", "_temp_meta.txt"));

        // Check if the metadata file exists, if not check for the temp metadata file
        if (!Files.exists(metaPath)) {
            if (Files.exists(tempMetaPath)) {
                metaPath = tempMetaPath;
            } else {
                throw new IOException("Metadata file not found: " + metaFilePath);
            }
        }

        Map<String, String> foreignKeys = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(metaPath);
        if (lines.size() > 3) { // Ensure the metadata file contains enough lines
            String relationshipLine = lines.get(3); // Extract the line containing relationships
            if (relationshipLine.startsWith("Relationship:")) {
                String[] relationships = relationshipLine.split(":")[1].split(",");
                for (String relationship : relationships) {
                    String[] parts = relationship.trim().split(" ");
                    if (parts.length == 4 && parts[0].equals("From") && parts[2].equals("to")) {
                        // Extract source table and column
                        String[] sourceParts = parts[1].split("\\(");
                        String sourceTable = sourceParts[0].trim();
                        String sourceColumn = sourceParts[1].replace(")", "").trim();

                        // Extract target table and column
                        String[] targetParts = parts[3].split("\\(");
                        String targetTable = targetParts[0].trim();
                        String targetColumn = targetParts[1].replace(")", "").trim();

                        // Format foreign key relationship
                        foreignKeys.put(sourceColumn.toLowerCase(), sourceColumn + " REFERENCES " + targetTable + "(" + targetColumn + ")");
                    } else {
                        throw new IllegalArgumentException("Invalid foreign key format in metadata file: " + relationship);
                    }
                }
            } else {
                System.out.println("No foreign key relationships found in metadata file.");
            }
        } else {
            System.out.println("Metadata file does not contain relationship information.");
        }
        return foreignKeys;
    }

    /**
     * Generates an SQL file containing the CREATE TABLE and INSERT INTO statements for the database.
     *
     * @param databaseName The name of the database to export.
     */
    public static void exportToSQL(String databaseName) {
        String basePath = "tinydb/databases";  // Path to the databases directory
        String exportFilePath = "export.sql";

        File baseDir = new File(basePath);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid base path: " + basePath);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(exportFilePath))) {
            File databaseDir = new File(baseDir, databaseName.toUpperCase());
            if (!databaseDir.exists() || !databaseDir.isDirectory()) {
                throw new IllegalArgumentException("Database directory not found: " + databaseDir.getPath());
            }

            File[] tableFiles = databaseDir.listFiles((dir, name) -> name.endsWith(".txt") && !name.endsWith("_meta.txt"));
            if (tableFiles == null) {
                throw new IllegalArgumentException("No table files found in database directory: " + databaseDir.getPath());
            }

            // Add logging before exporting data
            LogManager.logGeneral("EXPORT command initiated", "Attempting to export database: " + databaseName);

            for (File tableFile : tableFiles) {
                String tableName = tableFile.getName().replace(".txt", "");
                String metaFilePath = tableFile.getPath().replace(".txt", "_meta.txt");

                System.out.println("Processing table: " + tableName);
                System.out.println("Meta file path: " + metaFilePath);

                try {
                    Map<String, String> columnDetails = getColumnNamesAndTypes(metaFilePath);
                    Map<String, String> foreignKeys = getForeignKeys(metaFilePath);
                    List<String[]> tableData = getTableData(tableFile.getPath());

                    // Generate CREATE TABLE statement
                    writer.write("CREATE TABLE " + tableName + " (\n");
                    boolean firstColumn = true;
                    for (Map.Entry<String, String> entry : columnDetails.entrySet()) {
                        if (!firstColumn) {
                            writer.write(",\n");
                        }
                        writer.write("  " + entry.getKey() + " " + entry.getValue());
                        firstColumn = false;
                    }
                    // Add foreign key constraints
                    for (Map.Entry<String, String> entry : foreignKeys.entrySet()) {
                        writer.write(",\n  " + entry.getValue());
                    }
                    writer.write("\n);\n");

                    // Generate INSERT INTO statements if there is data
                    if (!tableData.isEmpty()) {
                        writer.write("INSERT INTO " + tableName + " (");
                        boolean firstColumnName = true;
                        for (String column : columnDetails.keySet()) {
                            if (!firstColumnName) {
                                writer.write(", ");
                            }
                            writer.write(column);
                            firstColumnName = false;
                        }
                        writer.write(") VALUES\n");

                        boolean firstRow = true;
                        for (String[] row : tableData) {
                            if (!firstRow) {
                                writer.write(",\n");
                            }
                            writer.write("(");
                            boolean firstValue = true;
                            for (String value : row) {
                                if (!firstValue) {
                                    writer.write(", ");
                                }
                                writer.write("'" + value.trim() + "'");
                                firstValue = false;
                            }
                            writer.write(")");
                            firstRow = false;
                        }
                        writer.write(";\n");
                    } else {
                        writer.write("-- No data to insert for table " + tableName + "\n");
                    }

                    writer.write("\n");  // Separate tables with a newline
                } catch (IOException e) {
                    System.out.println("Error processing table " + tableName + ": " + e.getMessage());
                }
            }

            System.out.println("Data exported successfully to " + exportFilePath);
            LogManager.logGeneral("EXPORT command executed", "Data exported successfully for database: " + databaseName);
        } catch (IOException e) {
            System.out.println("Error exporting data: " + e.getMessage());
            LogManager.logGeneral("EXPORT command failed", "Error exporting data for database: " + databaseName + " - " + e.getMessage());
        }
    }

    /**
     * The main method to initiate the export process.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java TinyDBExporter <databaseName>");
            System.exit(1);
        }
        String databaseName = args[0];
        exportToSQL(databaseName);
    }
}
