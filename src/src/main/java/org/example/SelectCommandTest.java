package org.example;

import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectCommandTest {

    private CommandProcessor commandProcessor;

    @BeforeAll
    void setUp() {
        commandProcessor = new CommandProcessor();
    }

    @BeforeEach
    void createTestDatabaseAndTable() throws Exception {
        // Create test database and table
        commandProcessor.processCommand("CREATE DATABASE testdb;");
        commandProcessor.processCommand("USE testdb;");
        commandProcessor.processCommand("CREATE TABLE people (id INT, name STRING, age INT);");

        // Insert sample data
        commandProcessor.processCommand("INSERT INTO people (id, name, age) VALUES (1, 'John Doe', 30);");
        commandProcessor.processCommand("INSERT INTO people (id, name, age) VALUES (2, 'Jane Doe', 25);");
        commandProcessor.processCommand("INSERT INTO people (id, name, age) VALUES (3, 'Jim Beam', 35);");
    }

    @Test
    void testSelectAllRecords() {
        assertDoesNotThrow(() -> {
            commandProcessor.processCommand("SELECT * FROM people;");
        });
    }


    @Test
    void testSelectWithCondition() {
        assertDoesNotThrow(() -> {
            commandProcessor.processCommand("SELECT * FROM people WHERE age > 30;");
        });
    }


    @Test
    void testSelectWithInvalidCondition() {
        Exception exception = assertThrows(Exception.class, () -> {
            commandProcessor.processCommand("SELECT * FROM people WHERE invalidColumn = 'test';");
        });

        assertFalse(exception.getMessage().contains("Invalid column"));
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
