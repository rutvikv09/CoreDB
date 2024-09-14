package org.example;

import org.junit.jupiter.api.*;

import java.io.File;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandProcessorTest {

    private CommandProcessor commandProcessor;

    @BeforeAll
    void setUp() {
        commandProcessor = new CommandProcessor();
    }

    @BeforeEach
    void cleanEnvironment() {
        deleteDirectory(new File("tinydb/databases/testdb"));
    }

    @Test
    void testCreateDatabase() {
        String command = "CREATE DATABASE testdb;";
        assertDoesNotThrow(() -> commandProcessor.processCommand(command));
    }

    @Test
    void testUseDatabase() {
        String createDbCommand = "CREATE DATABASE testdb;";
        String useDbCommand = "USE testdb;";

        assertDoesNotThrow(() -> {
            commandProcessor.processCommand(createDbCommand);
            commandProcessor.processCommand(useDbCommand);
        });
    }

    @AfterEach
    void tearDown() {
        deleteDirectory(new File("tinydb/databases/testdb"));
    }

    private void deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteDirectory(f);
                }
            }
        }
        file.delete();
    }
}
