package org.example;

import java.io.*;
import java.util.*;

public class CreateCommand {
    private static List<Relationship> globalRelationships = new ArrayList<>();

    /**
     * Executes the CREATE command to create a database or a table based on the input tokens.
     *
     * @param tokens The tokens parsed from the command input.
     * @param input The full command input.
     * @throws Exception If an error occurs during command execution.
     */
    public static void execute(String[] tokens, String input) throws Exception {
        if (tokens[1].equalsIgnoreCase("DATABASE")) {
            createDatabase(tokens[2]);
            LogManager.logEvent("CREATE DATABASE", "Database created: " + tokens[2]);
        } else if (tokens[1].equalsIgnoreCase("TABLE")) {
            createTable(input);
            LogManager.logEvent("CREATE TABLE", "Table created: " + tokens[2]);
        } else {
            throw new Exception("Invalid CREATE command");
        }
    }

    /**
     * Creates a table based on the input command.
     *
     * @param input The full command input.
     * @throws IOException If an I/O error occurs during table creation.
     */
    public static void createTable(String input) throws IOException {
        if (CommandProcessor.activeDatabase == null) {
            throw new IOException("No database selected.");
        }

        Scanner scanner = new Scanner(System.in);

        String[] tokens = input.split("\\s+");
        String tableName = tokens[2].trim();
        if (tableName.contains("(")) {
            tableName = tableName.substring(0, tableName.indexOf('('));
        }
        tableName = tableName.replace(";", "").trim().toUpperCase();

        String tableStructure = input.substring(input.indexOf('(') + 1, input.lastIndexOf(')')).trim();

        File dbDir = new File("tinydb/databases/" + CommandProcessor.activeDatabase);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        String[] columns = tableStructure.split(",");
        StringBuilder csvStructure = new StringBuilder();
        List<String> primaryKeys = new ArrayList<>();
        List<String> allColumns = new ArrayList<>();

        for (String column : columns) {
            String columnDef = column.trim();
            String[] columnParts = columnDef.split("\\s+");
            String columnName = columnParts[0].trim().toUpperCase();
            String columnType = columnParts[1].trim().toUpperCase();

            csvStructure.append(columnName).append(",");
            allColumns.add(columnName);

            if (columnDef.toLowerCase().contains("(pk)")) {
                primaryKeys.add(columnName);
            }
        }
        if (csvStructure.length() > 0) {
            csvStructure.setLength(csvStructure.length() - 1);
        }

        // Print table structure
        System.out.println("Table structure:");
        System.out.println("Columns: " + csvStructure.toString());

        // Ask user if they want to define relationships
        System.out.print("Do you want to define relationships for table " + tableName + "? (yes/no): ");
        String response = scanner.nextLine().trim().toLowerCase();

        if (response.equals("yes")) {
            String selectedColumn = promptForColumn(scanner, allColumns);
            System.out.println("Selected column: " + selectedColumn);

            List<String> tablesInDatabase = getTablesInDatabase(dbDir);
            tablesInDatabase.removeIf(name -> name.endsWith("_meta"));
            tablesInDatabase.remove(tableName);
            if (tablesInDatabase.isEmpty()) {
                System.out.println("There are no other tables in the current database for relationship.");
            } else {
                System.out.println("List of tables in the current database (excluding _meta tables):");
                for (String table : tablesInDatabase) {
                    System.out.println("Table: " + table);
                }

                String relatedTable = promptForValidTable(scanner, tablesInDatabase);

                if (relatedTable != null) {
                    List<String> relatedTableColumns = getTableColumns(dbDir, relatedTable);
                    System.out.println("Columns in table '" + relatedTable + "':");
                    for (String column : relatedTableColumns) {
                        System.out.println(column);
                    }

                    String relatedColumn = promptForRelatedColumn(scanner, relatedTableColumns);

                    boolean isPrimaryKey = isPrimaryKeyColumn(dbDir, relatedTable, relatedColumn);
                    while (!isPrimaryKey) {
                        System.out.println("The chosen column '" + relatedColumn + "' is not the primary key of table '" + relatedTable + "'.");
                        relatedColumn = promptForRelatedColumn(scanner, relatedTableColumns);
                        isPrimaryKey = isPrimaryKeyColumn(dbDir, relatedTable, relatedColumn);
                    }

                    // Store relationship without cardinality
                    Relationship relationship = new Relationship(tableName, relatedTable, selectedColumn, relatedColumn);
                    globalRelationships.add(relationship);

                    System.out.println("Relationship defined from column '" + selectedColumn + "' in table '" + tableName + "' to column '" + relatedColumn + "' in table '" + relatedTable + "'.");
                } else {
                    System.out.println("Invalid table name. Relationship not defined.");
                }
            }
        } else {
            System.out.println("No relationships defined for table " + tableName + ".");
        }

        // Write the table and metadata files
        writeTableFile(dbDir, tableName, csvStructure);
        writeMetaFile(dbDir, tableName, tableStructure, primaryKeys);
    }

    /**
     * Writes the table file with the given structure.
     *
     * @param dbDir The directory of the database.
     * @param tableName The name of the table.
     * @param csvStructure The CSV structure of the table columns.
     * @throws IOException If an I/O error occurs during file writing.
     */
    private static void writeTableFile(File dbDir, String tableName, StringBuilder csvStructure) throws IOException {
        String tableFilePath = dbDir + File.separator + tableName + ".txt";
        File tableFile = new File(tableFilePath);
        if (!tableFile.exists()) {
            tableFile.createNewFile();
            try (FileWriter tableWriter = new FileWriter(tableFile, false)) {
                tableWriter.write(csvStructure.toString() + "\n");
                System.out.println("Table " + tableName + " created.");
            }
        } else {
            System.out.println("Table " + tableName + " already exists.");
        }
    }

    /**
     * Writes the metadata file for the table.
     *
     * @param dbDir The directory of the database.
     * @param tableName The name of the table.
     * @param tableStructure The structure of the table.
     * @param primaryKeys The list of primary keys.
     * @throws IOException If an I/O error occurs during file writing.
     */
    private static void writeMetaFile(File dbDir, String tableName, String tableStructure, List<String> primaryKeys) throws IOException {
        String metaFilePath = dbDir + File.separator + tableName + "_meta.txt";
        File metaFile = new File(metaFilePath);
        if (!metaFile.exists()) {
            metaFile.createNewFile();
            try (FileWriter metaWriter = new FileWriter(metaFile)) {
                metaWriter.write("Table: " + tableName + "\n");
                metaWriter.write("Structure: " + tableStructure + "\n");
                for (String primaryKey : primaryKeys) {
                    metaWriter.write("Primary Key: " + primaryKey + "\n");
                }
                if (!globalRelationships.isEmpty()) {
                    for (Relationship relationship : globalRelationships) {
                        if (relationship.getTable().equalsIgnoreCase(tableName)) {
                            metaWriter.write("Relationship: " + relationship.toString() + "\n");
                        }
                    }
                }
                System.out.println("Metadata for table " + tableName + " created.");
            }
        } else {
            System.out.println("Metadata for table " + tableName + " already exists.");
        }
    }

    /**
     * Checks if the given column is a primary key in the specified table.
     *
     * @param dbDir The directory of the database.
     * @param tableName The name of the table.
     * @param columnName The name of the column.
     * @return true if the column is a primary key, false otherwise.
     */
    private static boolean isPrimaryKeyColumn(File dbDir, String tableName, String columnName) {
        String metaFilePath = dbDir + File.separator + tableName + "_meta.txt";
        File metaFile = new File(metaFilePath);
        if (metaFile.exists()) {
            try (Scanner scanner = new Scanner(metaFile)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("Primary Key:")) {
                        String primaryKey = line.split(":")[1].trim();
                        if (primaryKey.equalsIgnoreCase(columnName)) {
                            return true;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Creates a new database with the given name.
     *
     * @param dbName The name of the database to create.
     * @throws Exception If an error occurs during database creation.
     */
    public static void createDatabase(String dbName) throws Exception {
        dbName = dbName.replace(";", "").trim();
        File dbDir = new File("tinydb/databases/" + dbName.toUpperCase());
        if (!dbDir.exists()) {
            if (dbDir.mkdirs()) {
                System.out.println("Database created successfully.");
            } else {
                throw new Exception("Failed to create database.");
            }
        } else {
            throw new Exception("Database already exists.");
        }
    }

    /**
     * Returns the list of global relationships.
     *
     * @return The list of global relationships.
     */
    public static List<Relationship> getGlobalRelationships() {
        return globalRelationships;
    }

    /**
     * Retrieves the list of tables in the specified database directory.
     *
     * @param dbDir The directory of the database.
     * @return The list of table names.
     */
    private static List<String> getTablesInDatabase(File dbDir) {
        List<String> tables = new ArrayList<>();
        File[] files = dbDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".txt") && !file.getName().endsWith("_meta.txt")) {
                    String tableName = file.getName().replace(".txt", "").toUpperCase();
                    tables.add(tableName);
                }
            }
        }
        return tables;
    }

    /**
     * Retrieves the list of columns for the specified table.
     *
     * @param dbDir The directory of the database.
     * @param tableName The name of the table.
     * @return The list of column names.
     */
    private static List<String> getTableColumns(File dbDir, String tableName) {
        List<String> columns = new ArrayList<>();
        String tableFilePath = dbDir + File.separator + tableName + ".txt";
        File tableFile = new File(tableFilePath);
        if (tableFile.exists()) {
            try (Scanner scanner = new Scanner(tableFile)) {
                if (scanner.hasNextLine()) {
                    String headerLine = scanner.nextLine();
                    String[] columnNames = headerLine.split(",");
                    columns.addAll(Arrays.asList(columnNames));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return columns;
    }

    /**
     * Prompts the user to select a column from the provided list of columns.
     *
     * @param scanner The Scanner object for user input.
     * @param columns The list of available columns.
     * @return The selected column name.
     */
    private static String promptForColumn(Scanner scanner, List<String> columns) {
        System.out.println("Enter the column name for the relationship from table:");
        String selectedColumn = scanner.nextLine().trim().toUpperCase();
        while (!columns.contains(selectedColumn)) {
            System.out.println("Invalid column name. Please enter a valid column name:");
            selectedColumn = scanner.nextLine().trim().toUpperCase();
        }
        return selectedColumn;
    }

    /**
     * Prompts the user to select a valid table from the provided list of tables.
     *
     * @param scanner The Scanner object for user input.
     * @param tables The list of available tables.
     * @return The selected table name.
     */
    private static String promptForValidTable(Scanner scanner, List<String> tables) {
        System.out.println("Enter the name of the table to create a relationship with:");
        String tableName = scanner.nextLine().trim().toUpperCase();
        while (!tables.contains(tableName)) {
            System.out.println("Invalid table name. Please enter a valid table name:");
            tableName = scanner.nextLine().trim().toUpperCase();
        }
        return tableName;
    }

    /**
     * Prompts the user to select a column from the related table for the relationship.
     *
     * @param scanner The Scanner object for user input.
     * @param columns The list of available columns in the related table.
     * @return The selected related column name.
     */
    private static String promptForRelatedColumn(Scanner scanner, List<String> columns) {
        System.out.println("Enter the column name in the related table for the relationship:");
        String relatedColumn = scanner.nextLine().trim().toUpperCase();
        while (!columns.contains(relatedColumn)) {
            System.out.println("Invalid column name. Please enter a valid column name:");
            relatedColumn = scanner.nextLine().trim().toUpperCase();
        }
        return relatedColumn;
    }

    /**
     * Represents a relationship between two tables.
     */
    public static class Relationship {
        private String table;
        private String relatedTable;
        private String column;
        private String relatedColumn;
        private String cardinality;

        /**
         * Constructs a Relationship object with the specified parameters.
         *
         * @param table The name of the table.
         * @param relatedTable The name of the related table.
         * @param column The name of the column in the table.
         * @param relatedColumn The name of the column in the related table.
         */
        public Relationship(String table, String relatedTable, String column, String relatedColumn) {
            this.table = table;
            this.relatedTable = relatedTable;
            this.column = column;
            this.relatedColumn = relatedColumn;
            this.cardinality = cardinality;
        }

        /**
         * Returns the name of the table.
         *
         * @return The name of the table.
         */
        public String getTable() {
            return table;
        }

        /**
         * Returns the name of the related table.
         *
         * @return The name of the related table.
         */
        public String getRelatedTable() {
            return relatedTable;
        }

        /**
         * Returns the name of the column in the table.
         *
         * @return The name of the column in the table.
         */
        public String getColumn() {
            return column;
        }

        /**
         * Returns the name of the column in the related table.
         *
         * @return The name of the column in the related table.
         */
        public String getRelatedColumn() {
            return relatedColumn;
        }

        /**
         * Returns the cardinality of the relationship.
         *
         * @return The cardinality of the relationship.
         */
        public String getCardinality() {
            return cardinality;
        }

        /**
         * Returns a string representation of the relationship.
         *
         * @return A string representation of the relationship.
         */
        @Override
        public String toString() {
            return "From " + table + "(" + column + ") to " + relatedTable + "(" + relatedColumn + ")";
        }
    }
}
