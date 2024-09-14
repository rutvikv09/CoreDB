package org.example;

import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateCommandTest {

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
    void testUpdateSingleRecord() {
        assertDoesNotThrow(() -> {
            commandProcessor.processCommand("UPDATE people SET age = 31 WHERE id = 1;");
        });
    }


    @Test
    void testUpdateWithInvalidColumn() {
        Exception exception = assertThrows(Exception.class, () -> {
            commandProcessor.processCommand("UPDATE people SET invalidColumn = 50 WHERE id = 1;");
        });

        assertTrue(exception.getMessage().contains("Invalid column name in SET clause"));
    }

    @Test
    void testUpdateWithInvalidCondition() {
        Exception exception = assertThrows(Exception.class, () -> {
            commandProcessor.processCommand("UPDATE people SET age = 50 WHERE invalidColumn = 'test';");
        });

        assertTrue(exception.getMessage().contains("Invalid column name in WHERE clause"));
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
