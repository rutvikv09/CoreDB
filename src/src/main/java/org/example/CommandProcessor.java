package org.example;

public class CommandProcessor {
    public static String activeDatabase = null;
    public static String databaseNames;
    private static TransactionManager transactionManager = new TransactionManager();

    /**
     * Processes the input command by identifying its type and executing the appropriate action.
     *
     * @param input The input command to process.
     * @throws Exception If an error occurs during command processing.
     */
    public static void processCommand(String input) throws Exception {
        if (input.endsWith(";")) {
            input = input.substring(0, input.length() - 1);
        }
        String[] tokens = input.split("\\s+");
        String command = tokens[0].toUpperCase();

        if (transactionManager.isTransactionActive()) {
            if (command.equals("COMMIT") || command.equals("ROLLBACK")) {
                transactionManager.executeTransactionCommand(command);
            } else {
                transactionManager.execute(tokens, input);
            }
        } else {
            switch (command) {
                case "BEGIN":
                    if (tokens.length == 2 && tokens[1].equalsIgnoreCase("TRANSACTION")) {
                        transactionManager.beginTransaction();
                    } else {
                        throw new Exception("Invalid BEGIN command");
                    }
                    break;
                case "COMMIT":
                case "ROLLBACK":
                    throw new Exception("No active transaction");
                case "CREATE":
                    CreateCommand.execute(tokens, input);
                    LogManager.logGeneral("CREATE command executed", "Database state after CREATE");
                    break;
                case "USE":
                    UseCommand.execute(tokens);
                    LogManager.logGeneral("USE command executed", "Database state after USE");
                    break;
                case "INSERT":
                    InsertCommand.execute(tokens, input);
                    LogManager.logQuery(input, "Attempting to insert record");
                    break;
                case "SELECT":
                    SelectCommand.execute(tokens, input);
                    LogManager.logQuery(input, "Attempting to select record");
                    break;
                case "UPDATE":
                    UpdateCommand.execute(input);
                    LogManager.logQuery(input, "Attempting to update record");
                    break;
                case "DELETE":
                    DeleteCommand.execute(tokens);
                    LogManager.logQuery(input, "Attempting to delete record");
                    break;
                case "DROP":
                    DropCommand.execute(tokens);
                    LogManager.logGeneral("DROP command executed", "Database state after DROP");
                    break;
                default:
                    throw new Exception("Invalid command");
            }
        }
    }
}
