package org.example;

public class Validator {

    /**
     * Validates and formats the input command.
     * Ensures the command ends with a semicolon and converts it to uppercase.
     *
     * @param input The input command to validate and format.
     * @return The formatted command in uppercase.
     * @throws Exception If the command does not end with a semicolon.
     */
    public static String validateAndFormat(String input) throws Exception {
        // Check for semicolon at the end
        if (!input.endsWith(";")) {
            throw new Exception("Invalid command: Missing semicolon at the end.");
        }

        // Convert the command to uppercase
        return input.toUpperCase();
    }
}
