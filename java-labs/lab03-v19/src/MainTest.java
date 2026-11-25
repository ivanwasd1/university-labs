import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainIntegrationTest {

    @Test
    void fullProgramTest() throws Exception {
        String input = Files.readString(
                Path.of("test-data/input.txt")
        );

        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setIn(new ByteArrayInputStream(
                    input.getBytes(StandardCharsets.UTF_8)
            ));

            System.setOut(new PrintStream(
                    output, true, StandardCharsets.UTF_8
            ));

            Main.main(new String[0]);

        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }

        String result = output.toString(StandardCharsets.UTF_8);

        assertTrue(
                result.contains("бар <-> раб") ||
                        result.contains("раб <-> бар")
        );

        assertTrue(
                result.contains("кот <-> ток") ||
                        result.contains("ток <-> кот")
        );

        assertTrue(
                result.contains("топот <-> топот")
        );

        assertTrue(
                result.contains("дом <-> мод") ||
                        result.contains("мод <-> дом")
        );

        assertTrue(
                result.contains("а <-> а")
        );
    }
}