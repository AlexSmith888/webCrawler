package unitTests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import server.database.DbQueueItem;
import server.raw.RawLinksWorker;
import server.raw.RawQueueItem;
import server.transformation.TransformQueueItem;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.Assert.assertNull;

public class RawLinksWorkerTests {

    static int NUM_OF_THREADS = 1;
    ExecutorService rawPool;
    static int MAX_QUEUE_SIZE = 10000;
    static private final Logger logger = LogManager.getLogger(RawLinksWorkerTests.class);
    static int DEPTH = 5;
    static int HTTP_RESPONSE_TIME_OUT = 5;

    BlockingQueue<RawQueueItem> rawLinksQueue;
    BlockingQueue<TransformQueueItem> validLinksQueue;
    Set<String> seen;
    static TestHttpServer testHost;
    static CountDownLatch doneSignal;

    @BeforeAll
    public static void allocateResources() throws IOException {
        testHost = new TestHttpServer();
        testHost.start();
    }

    @BeforeEach
    void setUp() {
        rawPool = Executors.newFixedThreadPool(NUM_OF_THREADS);
        rawLinksQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        validLinksQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        seen = ConcurrentHashMap.newKeySet();
        doneSignal = new CountDownLatch(NUM_OF_THREADS);
    }

    @Test
    void processRawQueueDepth1() throws InterruptedException, URISyntaxException {
        String link = "http://localhost:8081/index.html";

        DEPTH = 1;
        rawLinksQueue.add(new RawQueueItem(link, link, DEPTH));
        new RawLinksWorkerTest().processRawQueue(rawLinksQueue, validLinksQueue, seen, rawPool,
                NUM_OF_THREADS, DEPTH, logger, HTTP_RESPONSE_TIME_OUT, doneSignal);
        Thread.sleep(HTTP_RESPONSE_TIME_OUT*1000 + 1000);

        Assertions.assertTrue(rawLinksQueue.isEmpty(),
                "rawLinksQueue Should not contain elements");
        Assertions.assertEquals(1, validLinksQueue.size(),
                "validLinksQueue Should contain one element");
        Assertions.assertEquals(1, seen.size(),
                "seenShould contain one element");;
        Assertions.assertEquals("http://localhost:8081/index.html", validLinksQueue.take().link()
                , "http://localhost:8081/.html is in the validLinksQueue");
    }

    @Test
    void processRawQueueDepth2() throws InterruptedException {
        String link = "http://localhost:8081/index.html";
        DEPTH = 2;

        rawLinksQueue.add(new RawQueueItem(link, link, 1));

        new RawLinksWorkerTest().processRawQueue(rawLinksQueue, validLinksQueue, seen, rawPool,
                NUM_OF_THREADS, DEPTH, logger, HTTP_RESPONSE_TIME_OUT, doneSignal);

        boolean completed = doneSignal.await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(completed, "Worker threads did not finish in time");

        Assertions.assertTrue(seen.contains("http://localhost:8081/index.html"));
        Assertions.assertTrue(seen.contains("http://localhost:8081/depth1.html"));
        Assertions.assertEquals(2, seen.size());

        Assertions.assertTrue(validLinksQueue.stream()
                .anyMatch(item -> item.link().equals("http://localhost:8081/index.html")));
        Assertions.assertTrue(validLinksQueue.stream()
                .anyMatch(item -> item.link().equals("http://localhost:8081/depth1.html")));
        Assertions.assertEquals(2, validLinksQueue.size());
    }
    @Test
    void processRawQueueDepth3() throws InterruptedException {
        String link = "http://localhost:8081/index.html";
        DEPTH = 3;

        rawLinksQueue.add(new RawQueueItem(link, link, 1));

        new RawLinksWorkerTest().processRawQueue(
                rawLinksQueue, validLinksQueue, seen, rawPool, NUM_OF_THREADS, DEPTH,
                logger, HTTP_RESPONSE_TIME_OUT, doneSignal
        );

        boolean completed = doneSignal.await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(completed, "Worker threads did not finish in time");
        Assertions.assertTrue(seen.contains("http://localhost:8081/index.html"), "Seen should contain index.html");
        Assertions.assertTrue(seen.contains("http://localhost:8081/depth1.html"), "Seen should contain depth1.html");
        Assertions.assertTrue(seen.contains("http://localhost:8081/depth2.html"), "Seen should contain depth2.html");
        Assertions.assertEquals(3, seen.size(), "Seen set should contain 3 elements");
        Assertions.assertTrue(validLinksQueue.stream().anyMatch(item -> item.link().equals("http://localhost:8081/index.html")));
        Assertions.assertTrue(validLinksQueue.stream().anyMatch(item -> item.link().equals("http://localhost:8081/depth1.html")));
        Assertions.assertTrue(validLinksQueue.stream().anyMatch(item -> item.link().equals("http://localhost:8081/depth2.html")));
        Assertions.assertEquals(3, validLinksQueue.size(), "Should contain 3 valid transformed items");
        Assertions.assertEquals(0, rawLinksQueue.size(), "Should contain 0 items");
    }


    @AfterEach
    public void shutdownPool() {
        rawPool.shutdown(); // Graceful shutdown
        try {
            if (!rawPool.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("Forcing shutdown...");
                rawPool.shutdownNow(); // Force shutdown
                if (!rawPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            rawPool.shutdownNow(); // Ensure shutdown
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
    }

    @AfterAll
    public static void shutdownResources() {
        testHost.stop();
    }
}
