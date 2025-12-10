package server.raw;

import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import server.requests.AppHtmlResponse;
import server.transformation.TransformQueueItem;
import server.utils.ValidationChecks;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

public class RawLinksWorker {
    /*
    RawLinksWorker :
    - Consume messages from the rawQueue
    - If the message has not been processed yet, continues
    - If an item reaches max depth, validates a link, and puts it in the validQueue without extracting child links
    - Extracts all child links, validates them, and puts them in the rawQueue
    * */
    public void processRawQueue (BlockingQueue<RawQueueItem> rawQueue,
                                 BlockingQueue<TransformQueueItem> validQueue, Set<String> seen, ExecutorService pool, int size, int DEPTH,
                                 Logger logger, int httpResponseTimeOut){
        Runnable task = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RawQueueItem item = rawQueue.take();
                    Document doc;

                    if (!seen.add(item.message())) {
                        continue;
                    }

                    if (item.level() >= DEPTH) {
                        doc = AppHtmlResponse.returnDoc(item, httpResponseTimeOut);
                        validQueue.put(new TransformQueueItem(item.message(), doc.toString()));
                        logger.debug("The final depth has been reached, " +
                                "no further processing for the next link : {}", item.message());
                        continue;
                    }

                    doc = AppHtmlResponse.returnDoc(item, httpResponseTimeOut);
                    validQueue.put(new TransformQueueItem(item.message(), doc.toString()));
                    logger.debug("The document has been extracted " +
                            "for the link : {}", item.message());

                    Elements links = doc.getElementsByTag("a");
                    for (var link : links) {
                        String next = link.attr("abs:href");
                        if (!ValidationChecks.linkIsValid(next, httpResponseTimeOut, logger)
                                || !ValidationChecks.linkIsSuccessor(item.parent(), next)
                        ) {
                            continue;
                        }
                        rawQueue.put(new RawQueueItem(item.parent(), next, item.level() + 1));
                        logger.debug("The child link {} has been put into the rawQueue " +
                                "for the link : {}", next, item.message());
                    }

                } catch (InterruptedException e) {
                    logger.warn("RawLinksWorker interrupted. Exiting ..");
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    logger.warn("IO error while processing rawQueue or parsing HTML");
                }
            }
        };
        for (int i = 0; i < size; i++) {
            pool.submit(task);
        }
    }
}
