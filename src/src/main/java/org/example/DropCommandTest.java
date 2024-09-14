package org.example;

import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DropCommandTest {

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

        // Verify that the table and metadata files are created
        assertTrue(Files.exists(Paths.get("tinydb/databases/testdb/people.txt")));
        assertTrue(Files.exists(Paths.get("tinydb/databases/testdb/people_meta.txt")));
    }

    @Test
    void testDropTable() {
        assertDoesNotThrow(() -> {
            commandProcessor.processCommand("DROP TABLE people;");
        });

        assertFalse(Files.exists(Paths.get("tinydb/databases/testdb/people.txt")));
        assertFalse(Files.exists(Paths.get("tinydb/databases/testdb/people_meta.txt")));
    }


    @Test
    void testDropTableAndMetadata() throws Exception {
        // Drop the table
        commandProcessor.processCommand("DROP TABLE people;");

        // Verify the table and metadata files are deleted
        assertFalse(Files.exists(Paths.get("tinydb/databases/testdb/people.txt")));
        assertFalse(Files.exists(Paths.get("tinydb/databases/testdb/people_meta.txt")));
    }

    @Test
    void testDropTableWithoutMetadata() throws Exception {
        // Manually delete the metadata file
        Files.deleteIfExists(Paths.get("tinydb/databases/testdb/people_meta.txt"));

        // Drop the table
        assertDoesNotThrow(() -> {
            commandProcessor.processCommand("DROP TABLE people;");
        });

        // Verify the table file is deleted
        assertFalse(Files.exists(Paths.get("tinydb/databases/testdb/people.txt")));
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
