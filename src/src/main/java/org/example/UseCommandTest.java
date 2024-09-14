package org.example;

import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UseCommandTest {

    private CommandProcessor commandProcessor;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeAll
    void setUp() {
        commandProcessor = new CommandProcessor();
        System.setOut(new PrintStream(outContent));
    }

    @BeforeEach
    void createTestDatabase() throws Exception {
        // Create test database
        commandProcessor.processCommand("CREATE DATABASE testdb;");
        outContent.reset();
    }



    @Test
    void testUseNonExistentDatabase() throws Exception {
        commandProcessor.processCommand("USE nonExistentDb;");
        String expectedMessage = "Database nonExistentDb does not exist.";
        assertTrue(outContent.toString().contains(expectedMessage), "Expected message not found in output.");
    }

    @Test
    void testUseDatabaseWithoutSemicolon() throws Exception {
        commandProcessor.processCommand("USE testdb");
        String expectedMessage = "Invalid USE command syntax.";
        assertFalse(outContent.toString().contains(expectedMessage), "Expected message not found in output.");
    }

    @AfterEach
    void tearDown() {
        deleteDirectory(new File("tinydb/databases/testdb"));
        outContent.reset();
    }

    @AfterAll
    void restoreSystemOut() {
        System.setOut(originalOut);
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
