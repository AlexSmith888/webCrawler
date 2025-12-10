package server.database;

import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class FileLinksWorker {
    /*
    DatabaseLinksWorker
        - Consumes the database queue
        - Loads a JSON file to the database
    */
    public void savePreparedItem (BlockingQueue<DbQueueItem> databaseQueue, Logger logger, Set<String> seen, MongoDatabase DB){
        Runnable writer = () -> {
            try (BufferedWriter writerOut = new BufferedWriter(new FileWriter("src/main/resources/output.txt", true))) {
                while (!Thread.currentThread().isInterrupted()) {
                    DbQueueItem line = databaseQueue.take();
                    //writerOut.write(line.json());
                    //writerOut.write(line.json());
                    writerOut.newLine();
                    writerOut.flush();
                    logger.debug("The link {} has been loaded", line.link());
                }
            } catch (InterruptedException e) {
                logger.warn("DatabaseLinksWorker interrupted. Exiting ..");
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                logger.warn("Failed to load to a database");
            }
        };
        new Thread(writer, "FileWriterThread").start();
    }
}

