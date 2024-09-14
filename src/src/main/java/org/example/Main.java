
package org.example;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

import static org.example.CommandProcessor.activeDatabase;
import static org.example.ERDExport.exportERD;

public class Main {

    private static UserProfileManager userProfileManager = new UserProfileManager();
    private static CommandProcessor commandProcessor = new CommandProcessor();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Map<String, String[]> userProfiles = userProfileManager.loadUserProfiles();

        while (true) {
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline left-over

            switch (choice) {
                case 1:
                    register(scanner, userProfiles);
                    break;
                case 2:
                    login(scanner, userProfiles);
                    break;
                case 3:
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void register(Scanner scanner, Map<String, String[]> userProfiles) {
        System.out.print("Enter userID: ");
        String userID = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        String hashedUserID = UserProfileManager.hashString(userID);
        String hashedPassword = UserProfileManager.hashString(password);

        if (userProfiles.containsKey(hashedUserID)) {
            System.out.println("User already exists.");
            return;
        }

        System.out.print("Enter security question: ");
        String securityQuestion = scanner.nextLine();
        System.out.print("Enter security answer: ");
        String securityAnswer = scanner.nextLine();

        userProfiles.put(hashedUserID, new String[]{hashedPassword, securityQuestion, securityAnswer});
        userProfileManager.saveUserProfiles(userProfiles);
        System.out.println("Registration successful!");
        LogManager.logUserActivity("REGISTER", userID);
    }

    private static void login(Scanner scanner, Map<String, String[]> userProfiles) {
        System.out.print("Enter userID: ");
        String userID = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        String hashedUserID = UserProfileManager.hashString(userID);
        String hashedPassword = UserProfileManager.hashString(password);

        if (userProfiles.containsKey(hashedUserID) && userProfiles.get(hashedUserID)[0].equals(hashedPassword)) {
            System.out.println("Answer the security question " + userProfiles.get(hashedUserID)[1] + ":");
            String securityAnswer = scanner.nextLine();
            if (userProfiles.get(hashedUserID)[2].equals(securityAnswer)) {
                System.out.println("Login successful!");
                LogManager.logUserActivity("LOGIN", userID);
                accessSystem(scanner);
            } else {
                System.out.println("Security answer incorrect.");
                LogManager.logUserActivity("LOGIN_FAILED", userID);
            }
        } else {
            System.out.println("Invalid userID or password.");
            LogManager.logUserActivity("LOGIN_FAILED", userID);
        }
    }

    private static void accessSystem(Scanner scanner) {
        System.out.println("Welcome to TinyDB!");

        while (true) {
            System.out.println("1. Write Queries");
            System.out.println("2. Export Data and Structure");
            System.out.println("3. ERD");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline left-over

            switch (choice) {
                case 1:
                    writeQueries(scanner);
                    break;
                case 2:
                    try {
                        exportDataAndStructure(scanner);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case 3:
                    System.out.print("Enter the database name for ERD export: ");
                    String dbName = scanner.nextLine().trim().toUpperCase();
                    ERDExport.exportERD(dbName);
                    break;
                case 4:
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void writeQueries(Scanner scanner) {
        while (true) {
            System.out.print("TinyDB> ");
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.equalsIgnoreCase("exit")) {
                break;
            } else if (input.equalsIgnoreCase("HELP")) {
                showHelp();
                continue;
            }

            try {
                commandProcessor.processCommand(input);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void showHelp() {
        System.out.println("Available commands:");
        System.out.println("1. CREATE DATABASE <dbName>;");
        System.out.println("2. USE <dbName>;");
        System.out.println("3. CREATE TABLE <tableName> (column1 type1, column2 type2, ...);");
        System.out.println("4. INSERT INTO <tableName> (column1, column2, ...) VALUES (value1, value2, ...);");
        System.out.println("5. SELECT * FROM <tableName>;");
        System.out.println("6. SELECT <column1>, <column2> FROM <tableName>;");
        System.out.println("7. SELECT * FROM <tableName> WHERE <column> <operator> <value>;");
        System.out.println("8. UPDATE <tableName> SET <column> = <value> WHERE <column> = <value>;");
        System.out.println("9. DELETE FROM <tableName> WHERE <column> = <value>;");
        System.out.println("10. DROP TABLE <tableName>;");
    }

    private static void exportDataAndStructure(Scanner scanner) throws IOException {
        System.out.print("Enter the database name to export: ");
        String databaseName = scanner.nextLine().trim();

        File databaseDir = new File("tinydb/databases/" + databaseName.toUpperCase());
        if (!databaseDir.exists() || !databaseDir.isDirectory()) {
            System.out.println("Database not found: " + databaseName);
            return;
        }

        TinyDBExporter.exportToSQL(databaseName.toUpperCase());
        System.out.println("Data exported successfully to export.sql");
    }

}