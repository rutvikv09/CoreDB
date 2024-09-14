package org.example;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteCommandTest {

    private CommandProcessor commandProcessor;

    @BeforeAll
    void setUp() {
        commandProcessor = new CommandProcessor();
    }

    @BeforeEach
    void createTestDatabase() throws Exception {
        // Create test database and table
        commandProcessor.processCommand("CREATE DATABASE testdb;");
        commandProcessor.processCommand("USE testdb;");
        commandProcessor.processCommand("CREATE TABLE people (id INT, name STRING, age INT);");

        // Insert test data
        String tableFilePath = "tinydb/databases/testdb/people.txt";
        try (FileWriter writer = new FileWriter(tableFilePath, true)) {
            writer.write("1,John,30\n");
            writer.write("2,Jane,25\n");
        }
    }

    @Test
    void testDeleteRecord() {
        assertDoesNotThrow(() -> {
            commandProcessor.processCommand("DELETE FROM people WHERE id=1;");
        });

        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get("tinydb/databases/testdb/people.txt"));
        } catch (IOException e) {
            fail("Error reading table file.");
            return;
        }

        assertEquals(2, lines.size()); // Including header
        assertFalse(lines.stream().anyMatch(line -> line.contains("1,John,30")));
    }

    @Test
    void testDeleteRecordNoMatch() {
        assertDoesNotThrow(() -> {
            commandProcessor.processCommand("DELETE FROM people WHERE id=3;");
        });

        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get("tinydb/databases/testdb/people.txt"));
        } catch (IOException e) {
            fail("Error reading table file.");
            return;
        }

        assertEquals(3, lines.size()); // Including header and two records
    }

    @Test
    void testDeleteRecordInvalidTable() {
        Exception exception = assertThrows(Exception.class, () -> {
            commandProcessor.processCommand("DELETE FROM invalidTable WHERE id=1;");
        });

        assertEquals("Table does not exist.", exception.getMessage());
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
