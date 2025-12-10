package unitTests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import server.utils.ValidationChecks;
import unitTests.TestHttpServer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class ValidationChecksTest {

    static TestHttpServer testServer;
    Logger logger = LogManager.getLogger(ValidationChecksTest.class);

    @BeforeAll
    static void startServer() throws IOException, InterruptedException {
        testServer = new TestHttpServer();
        testServer.start();
    }

    @AfterAll
    static void stopServer() {
        testServer.stop();
    }

    @Test
    void testValidLinkReturnsTrue() {
        String url = "http://localhost:8081/index.html";
        boolean result = ValidationChecks.linkIsValid(url, 5, logger);
        assertTrue(result);
    }

    @Test
    void testInvalidLinkReturnsFalse() {
        String url = "http://localhost:8081/notfound.html";
        boolean result = ValidationChecks.linkIsValid(url, 5, logger);
        assertFalse(result);
    }

    @Test
    void testLinkWithInvalidSchemeReturnsFalse() {
        String url = "ftp://localhost:8081/index.html";
        boolean result = ValidationChecks.linkIsValid(url, 5, logger);
        assertFalse(result);
    }
}
