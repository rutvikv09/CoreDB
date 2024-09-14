package org.example;

import java.io.*;
import java.util.*;

public class ERDExport {

    /**
     * Exports the Entity-Relationship Diagram (ERD) for the specified database.
     *
     * @param dbName The name of the database to export the ERD for.
     */
    public static void exportERD(String dbName) {
        File dbDir = new File("tinydb/databases/" + dbName);
        if (!dbDir.exists()) {
            System.out.println("Active database not found.");
            return;
        }

        File[] files = dbDir.listFiles();
        if (files != null) {
            List<CreateCommand.Relationship> relationships = new ArrayList<>();
            Map<String, TableInfo> tableInfoMap = new HashMap<>();
            try {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith("_meta.txt")) {
                        TableInfo tableInfo = readTableInfo(file);
                        tableInfoMap.put(tableInfo.tableName.toUpperCase(), tableInfo);
                        // Collect relationships from each meta file
                        relationships.addAll(tableInfo.relationships);
                    }
                }

                File erdFile = new File(dbDir, "ERD.txt");
                // Clear the file by opening it in write mode
                try (FileWriter fileWriter = new FileWriter(erdFile, false); // 'false' to overwrite
                     PrintWriter erdWriter = new PrintWriter(fileWriter)) {
                    // Print database name
                    erdWriter.println("Entity-Relationship Diagram for database: " + dbName);
                    erdWriter.println();

                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".txt") && !file.getName().endsWith("_meta.txt")) {
                            String tableName = file.getName().replace(".txt", "").toUpperCase();
                            erdWriter.println("Table: " + tableName);

                            // Print column names
                            printColumnNames(erdWriter, tableInfoMap.get(tableName));

                            // Print relationships
                            writeRelationships(erdWriter, tableName, relationships, tableInfoMap);

                            erdWriter.println();
                        }
                    }

                    System.out.println("ERD exported successfully.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No tables found in the active database.");
        }
    }

    /**
     * Reads the table information from the specified metadata file.
     *
     * @param metaFile The metadata file to read the table information from.
     * @return The table information.
     * @throws IOException If an I/O error occurs.
     */
    private static TableInfo readTableInfo(File metaFile) throws IOException {
        TableInfo tableInfo = new TableInfo();
        try (Scanner scanner = new Scanner(metaFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("Table:")) {
                    tableInfo.tableName = line.split(":")[1].trim();
                } else if (line.startsWith("Structure:")) {
                    String[] columns = line.split(":")[1].trim().split(",");
                    for (String column : columns) {
                        tableInfo.columns.add(column.trim().split("\\s+")[0].toUpperCase());
                    }
                } else if (line.startsWith("Primary Key:")) {
                    tableInfo.primaryKeys.add(line.split(":")[1].trim().toUpperCase());
                } else if (line.startsWith("Relationship:")) {
                    String[] parts = line.split(":")[1].trim().split(" to ");
                    String[] from = parts[0].split("\\(");
                    String[] to = parts[1].split("\\(");

                    String fromColumn = from[1].replace(")", "").trim();
                    String toColumn = to[1].replace(")", "").trim();

                    tableInfo.relationships.add(new CreateCommand.Relationship(
                            tableInfo.tableName,
                            to[0].trim(),
                            fromColumn,
                            toColumn
                    ));
                }
            }
        }
        return tableInfo;
    }

    /**
     * Prints the column names and primary keys for the specified table.
     *
     * @param writer The writer to print to.
     * @param tableInfo The table information.
     */
    private static void printColumnNames(PrintWriter writer, TableInfo tableInfo) {
        writer.println("Columns: " + String.join(", ", tableInfo.columns));
        writer.println("Primary Keys: " + String.join(", ", tableInfo.primaryKeys));
    }

    /**
     * Writes the relationships for the specified table.
     *
     * @param writer The writer to print to.
     * @param tableName The name of the table.
     * @param relationships The list of relationships.
     * @param tableInfoMap The map of table information.
     */
    private static void writeRelationships(PrintWriter writer, String tableName, List<CreateCommand.Relationship> relationships, Map<String, TableInfo> tableInfoMap) {
        boolean foundRelationship = false;
        for (CreateCommand.Relationship relationship : relationships) {
            if (relationship.getTable().equalsIgnoreCase(tableName)) {
                String relatedTable = relationship.getRelatedTable();
                String column = relationship.getColumn();
                String relatedColumn = relationship.getRelatedColumn();

                boolean isPrimaryKeyTable = isPrimaryKeyColumn(tableInfoMap, tableName, column);
                boolean isPrimaryKeyRelatedTable = isPrimaryKeyColumn(tableInfoMap, relatedTable, relatedColumn);
                String cardinality = determineCardinality(isPrimaryKeyTable, isPrimaryKeyRelatedTable);

                writer.println("Relationship: " + tableName + " (" + column + ") -> " + relatedTable + " (" + relatedColumn + ") - Cardinality: " + cardinality);
                foundRelationship = true;
            }
        }
        if (!foundRelationship) {
            writer.println("No relationships found for this table.");
        }
    }

    /**
     * Checks if the specified column is a primary key in the specified table.
     *
     * @param tableInfoMap The map of table information.
     * @param tableName The name of the table.
     * @param columnName The name of the column.
     * @return true if the column is a primary key, false otherwise.
     */
    private static boolean isPrimaryKeyColumn(Map<String, TableInfo> tableInfoMap, String tableName, String columnName) {
        TableInfo tableInfo = tableInfoMap.get(tableName.toUpperCase());
        return tableInfo != null && tableInfo.primaryKeys.contains(columnName.toUpperCase());
    }

    /**
     * Determines the cardinality of the relationship.
     *
     * @param isPrimaryKeyTable true if the column in the table is a primary key.
     * @param isPrimaryKeyRelatedTable true if the column in the related table is a primary key.
     * @return The cardinality of the relationship.
     */
    private static String determineCardinality(boolean isPrimaryKeyTable, boolean isPrimaryKeyRelatedTable) {
        if (isPrimaryKeyTable && isPrimaryKeyRelatedTable) {
            return "1-to-1";
        } else if (isPrimaryKeyTable || isPrimaryKeyRelatedTable) {
            return "many-to-1";
        } else {
            return "unknown";
        }
    }

    /**
     * Represents the information of a table, including columns, primary keys, and relationships.
     */
    static class TableInfo {
        String tableName;
        List<String> columns = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();
        List<CreateCommand.Relationship> relationships = new ArrayList<>();
    }
}
