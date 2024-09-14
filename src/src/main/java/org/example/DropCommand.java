package org.example;

import java.io.File;

public class DropCommand {

    /**
     * Executes the DROP command to drop a table and its metadata.
     *
     * @param tokens The tokens parsed from the command input.
     * @throws Exception If an error occurs during command execution.
     */
    public static void execute(String[] tokens) throws Exception {
        if (CommandProcessor.activeDatabase == null) {
            throw new Exception("No database selected.");
        }

        // Add logging before dropping the table
        LogManager.logGeneral("DROP command initiated", "Attempting to drop table");

        if (tokens.length < 3) {
            System.out.println("Invalid DROP command syntax.");
            return;
        }

        String tableName = tokens[2].replace(";", "").trim();
        File tableFile = new File("tinydb/databases/" + CommandProcessor.activeDatabase + "/" + tableName + ".txt");
        File metaFile = new File("tinydb/databases/" + CommandProcessor.activeDatabase + "/" + tableName + "_meta.txt");

        boolean tableDropped = false;
        boolean metaDropped = false;

        if (tableFile.exists()) {
            if (tableFile.delete()) {
                tableDropped = true;
            } else {
                throw new Exception("Failed to drop table.");
            }
        } else {
            System.out.println("Table " + tableName + " does not exist.");
        }

        if (metaFile.exists()) {
            if (metaFile.delete()) {
                metaDropped = true;
            } else {
                throw new Exception("Failed to drop metadata.");
            }
        } else {
            System.out.println("Metadata for table " + tableName + " does not exist.");
        }

        if (tableDropped && metaDropped) {
            System.out.println("Table and metadata dropped successfully.");
            LogManager.logGeneral("DROP command executed", "Table and metadata dropped successfully for table: " + tableName);
        } else if (tableDropped) {
            System.out.println("Table dropped successfully, but metadata was not found.");
            LogManager.logGeneral("DROP command executed", "Table dropped successfully, but metadata was not found for table: " + tableName);
        } else if (metaDropped) {
            System.out.println("Metadata dropped successfully, but table was not found.");
            LogManager.logGeneral("DROP command executed", "Metadata dropped successfully, but table was not found for table: " + tableName);
        }
    }
}
