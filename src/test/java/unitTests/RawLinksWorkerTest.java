package unitTests;

import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import server.raw.RawQueueItem;
import server.requests.AppHtmlResponse;
import server.transformation.TransformQueueItem;
import server.utils.ValidationChecks;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class RawLinksWorkerTest {
    public void processRawQueue(
            BlockingQueue<RawQueueItem> rawQueue,
            BlockingQueue<TransformQueueItem> validQueue,
            Set<String> seen,
            ExecutorService pool,
            int size,
            int DEPTH,
            Logger logger,
            int httpResponseTimeOut,
            CountDownLatch doneSignal
    ) {
        Runnable task = () -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    RawQueueItem item = rawQueue.poll(500, TimeUnit.MILLISECONDS);
                    if (item == null) {
                        if (rawQueue.isEmpty()) break;
                        continue;
                    }

                    if (!seen.add(item.message())) {
                        continue;
                    }

                    Document doc = AppHtmlResponse.returnDoc(item, httpResponseTimeOut);
                    validQueue.put(new TransformQueueItem(item.message(), doc.toString()));
                    logger.debug("Processed: {}", item.message());

                    // Enqueue children only if depth allows
                    if (item.level() < DEPTH) {
                        Elements links = doc.getElementsByTag("a");
                        for (var link : links) {
                            String next = link.attr("abs:href");
                            if (!ValidationChecks.linkIsValid(next, httpResponseTimeOut, logger)) {
                                continue;
                            }
                            rawQueue.put(new RawQueueItem(item.parent(), next, item.level() + 1));
                            logger.debug("Enqueued child: {}", next);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Worker failed", e);
            } finally {
                doneSignal.countDown();
            }
        };

        for (int i = 0; i < size; i++) {
            pool.submit(task);
        }
    }
}

