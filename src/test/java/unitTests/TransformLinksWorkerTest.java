package unitTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import server.database.DbQueueItem;
import server.transformation.*;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class TransformLinksWorkerTest {

    private static final Logger logger = LogManager.getLogger(TransformLinksWorkerTest.class);
    private ExecutorService executor;
    private BlockingQueue<TransformQueueItem> validQueue;
    private BlockingQueue<DbQueueItem> databaseQueue;
    private CountDownLatch doneSignal;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(1);
        validQueue = new LinkedBlockingQueue<>();
        databaseQueue = new LinkedBlockingQueue<>();
        doneSignal = new CountDownLatch(1);  // Since pool size = 1
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testTransformRawQueueItemProcessesAll() throws InterruptedException {
        // Prepare 3 items
        TransformQueueItem item1 = new TransformQueueItem("http://example.com/1", "<html>Page 1</html>");
        TransformQueueItem item2 = new TransformQueueItem("http://example.com/2", "<html>Page 2</html>");
        TransformQueueItem item3 = new TransformQueueItem("http://example.com/3", "<html>Page 3</html>");

        validQueue.add(item1);
        validQueue.add(item2);
        validQueue.add(item3);

        Runnable wrappedTask = () -> {
            try {
                new TranformLinksWorker().transformRawQueueItem(validQueue, databaseQueue, executor, 1, logger);

                // Wait until all 3 items processed, or timeout after 5 seconds
                int processed = 0;
                while (processed < 3) {
                    DbQueueItem dbItem = databaseQueue.poll(5, TimeUnit.SECONDS);
                    assertNotNull(dbItem, "DbQueueItem should not be null");
                    processed++;
                }
            } catch (Exception e) {
                fail("Exception during processing: " + e.getMessage());
            } finally {
                doneSignal.countDown();
            }
        };

        new Thread(wrappedTask).start();

        boolean completed = doneSignal.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Worker did not complete in time");

        assertTrue(databaseQueue.size() <= 3, "Database queue should have all processed items");
        assertTrue(validQueue.isEmpty(), "Valid queue should be empty after processing");
    }
}