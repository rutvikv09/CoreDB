package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionManager {
    private boolean isTransactionActive = false;
    private static final String BUFFER_FILE = "transaction_buffer.txt";

    /**
     * Checks if a transaction is active.
     *
     * @return True if a transaction is active, false otherwise.
     */
    public boolean isTransactionActive() {
        return isTransactionActive;
    }

    /**
     * Begins a new transaction.
     */
    public void beginTransaction() {
        if (!isTransactionActive) {
            isTransactionActive = true;
            clearBufferFile();
            System.out.println("Transaction started.");
            LogManager.logTransaction("BEGIN", "Transaction started.");
        } else {
            System.out.println("Transaction already in progress.");
        }
    }

    /**
     * Executes a command as part of a transaction.
     *
     * @param tokens The tokens parsed from the command input.
     * @param input The complete command input string.
     * @throws Exception If an error occurs during command execution.
     */
    public void execute(String[] tokens, String input) throws Exception {
        String command = tokens[0].toUpperCase();

        if (command.equals("SELECT")) {
            // Allow SELECT operations to execute immediately
            SelectCommand.execute(tokens, input);
        } else if (command.equals("INSERT") || command.equals("DELETE") || command.equals("UPDATE")) {
            // Validate the command before adding to the buffer
            if (validateCommand(tokens, input)) {
                appendToBufferFile(input);
                System.out.println("Operation added to transaction: " + input);
                LogManager.logTransaction("EXECUTE", "Operation added to transaction: " + input);
            } else {
                System.out.println("Error: Invalid " + command + " statement format: " + input);
            }
        } else {
            appendToBufferFile(input);
            System.out.println("Operation added to transaction: " + input);
            LogManager.logTransaction("EXECUTE", "Operation added to transaction: " + input);
        }
    }

    /**
     * Executes a transaction command (COMMIT or ROLLBACK).
     *
     * @param command The transaction command to execute.
     * @throws Exception If an error occurs during command execution or if no active transaction exists.
     */
    public void executeTransactionCommand(String command) throws Exception {
        if (!isTransactionActive) {
            throw new Exception("No active transaction.");
        }

        if (command.equalsIgnoreCase("COMMIT")) {
            commitTransaction();
        } else if (command.equalsIgnoreCase("ROLLBACK")) {
            rollbackTransaction();
        } else {
            throw new Exception("Invalid transaction command: " + command);
        }
    }

    /**
     * Commits the current transaction by executing all buffered operations.
     *
     * @throws Exception If an error occurs during transaction commit.
     */
    private void commitTransaction() throws Exception {
        try {
            List<String> operations = readBufferFile();
            for (String operation : operations) {
                executeOperation(operation);
            }
            isTransactionActive = false;
            clearBufferFile();
            System.out.println("Transaction committed.");
            LogManager.logTransaction("COMMIT", "Transaction committed.");
        } catch (Exception e) {
            rollbackTransaction();
            throw new Exception("Transaction failed, rolled back. Error: " + e.getMessage());
        }
    }

    /**
     * Executes an individual operation as part of a transaction.
     *
     * @param operation The operation to execute.
     * @throws Exception If an error occurs during operation execution.
     */
    private void executeOperation(String operation) throws Exception {
        String[] tokens = operation.split("\\s+");
        String command = tokens[0].toUpperCase();

        switch (command) {
            case "CREATE":
                CreateCommand.execute(tokens, operation);
                break;
            case "INSERT":
                InsertCommand.execute(tokens, operation);
                break;
            case "UPDATE":
                UpdateCommand.execute(operation);
                break;
            case "DELETE":
                DeleteCommand.execute(tokens);
                break;
            case "DROP":
                DropCommand.execute(tokens);
                break;
            default:
                throw new Exception("Invalid operation in transaction: " + operation);
        }
    }

    /**
     * Rolls back the current transaction, discarding all buffered operations.
     */
    private void rollbackTransaction() {
        isTransactionActive = false;
        clearBufferFile();
        System.out.println("Transaction rolled back.");
        LogManager.logTransaction("ROLLBACK", "Transaction rolled back.");
    }

    /**
     * Validates a command before adding it to the transaction buffer.
     *
     * @param tokens The tokens parsed from the command input.
     * @param input The complete command input string.
     * @return True if the command is valid, false otherwise.
     */
    private boolean validateCommand(String[] tokens, String input) {
        String command = tokens[0].toUpperCase();
        switch (command) {
            case "INSERT":
                return input.matches("INSERT INTO [a-zA-Z0-9_]+\\s*\\([a-zA-Z0-9_,\\s]*\\)\\s*VALUES\\s*\\([a-zA-Z0-9_,\\s\"']*\\);?");
            case "DELETE":
                return input.matches("DELETE FROM [a-zA-Z0-9_]+\\s*WHERE\\s*[a-zA-Z0-9_\\s=]*;?");
            case "UPDATE":
                return input.matches("UPDATE [a-zA-Z0-9_]+\\s*SET\\s*[a-zA-Z0-9_\\s=,\"]+\\s*WHERE\\s*[a-zA-Z0-9_\\s=]*;?");
            default:
                return true; // Allow other commands without validation
        }
    }

    /**
     * Appends an operation to the transaction buffer file.
     *
     * @param operation The operation to append.
     */
    private void appendToBufferFile(String operation) {
        try (FileWriter fw = new FileWriter(BUFFER_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(operation);
        } catch (IOException e) {
            System.err.println("Error writing to buffer file: " + e.getMessage());
        }
    }

    /**
     * Reads all operations from the transaction buffer file.
     *
     * @return A list of operations.
     */
    private List<String> readBufferFile() {
        List<String> operations = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(BUFFER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                operations.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading from buffer file: " + e.getMessage());
        }
        return operations;
    }

    /**
     * Clears the transaction buffer file.
     */
    private void clearBufferFile() {
        try {
            new FileWriter(BUFFER_FILE, false).close();
        } catch (IOException e) {
            System.err.println("Error clearing buffer file: " + e.getMessage());
        }
    }
}
