package unitTests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import server.WebServer;
import server.raw.Initialization;
import server.raw.RawQueueItem;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class InitializationTests {
    @Test
    public void checkMainLogictest() throws IOException, InterruptedException {
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientOut);

        PipedOutputStream serverOut = new PipedOutputStream();
        PipedInputStream clientIn = new PipedInputStream(serverOut);

        Socket serverSocketStub = new Socket() {
            @Override
            public InputStream getInputStream() {
                return serverIn;
            }

            @Override
            public OutputStream getOutputStream() {
                return serverOut;
            }

            @Override
            public void shutdownOutput() {
            }
        };

        BlockingQueue<RawQueueItem> queue = new LinkedBlockingQueue<>();
        Logger logger = LogManager.getLogger(InitializationTests.class);
        Initialization init = new Initialization(serverSocketStub,
                queue, 5, logger);
        new Thread(init).start();

        clientOut.write("https://books.toscrape.com".getBytes(StandardCharsets.UTF_8));
        clientOut.close();

        ByteArrayOutputStream response = new ByteArrayOutputStream();
        int val;
        while ((val = clientIn.read()) != -1) {
            response.write(val);
        }

        String answer = response.toString(StandardCharsets.UTF_8);
        Assertions.assertTrue(answer.contains("Request is being processed"));

        RawQueueItem item = queue.poll(2, TimeUnit.SECONDS);
        Assertions.assertNotNull(item);
        Assertions.assertEquals("https://books.toscrape.com", item.message());
    }
}

