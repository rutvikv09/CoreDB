package org.example;

import java.io.File;
import java.io.IOException;

public class UseCommand {

    /**
     * Executes the USE command to set the active database.
     * Validates the command syntax and checks if the database exists.
     *
     * @param tokens The tokens parsed from the command input.
     * @throws IOException If an I/O error occurs.
     */
    public static void execute(String[] tokens) throws IOException {
        if (tokens.length < 2) {
            System.out.println("Invalid USE command syntax.");
            return;
        }
        String dbName = tokens[1].replace(";", "").trim();
        File dbDir = new File("tinydb/databases/" + dbName);
        if (dbDir.exists() && dbDir.isDirectory()) {
            CommandProcessor.activeDatabase = dbName;
            System.out.println("Using database: " + dbName);
        } else {
            System.out.println("Database " + dbName + " does not exist.");
        }
    }
}
