package server.database;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.Logger;
import server.utils.ProjectVariables;
import org.bson.Document;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

public class DatabaseLinksWorker {
    /*
    DatabaseLinksWorker
        - Consumes the database queue
        - Loads a JSON file to the database
    */
    public void savePreparedItem (BlockingQueue<DbQueueItem> databaseQueue, Logger logger, Set<String> seen, MongoDatabase DB){
        Runnable writer = () -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {

                    MongoCollection<Document> collection = DB.getCollection(ProjectVariables.COLLECTION.label);
                    DbQueueItem item = databaseQueue.take();
                    String uid = item.json().get("uid").asText();
                    String contentHash = item.json().get("content_hash").asText();

                    Document existing = collection.find(eq("uid", uid))
                            .sort(descending("version"))
                            .limit(1)
                            .first();

                    if (existing != null) {
                        String existingHash = existing.getString("content_hash");
                        if (existingHash.equals(contentHash)) {
                            logger.debug("Duplicate content for UID {} — skipping", uid);
                            continue;
                        }
                        int newVersion = existing.getInteger("version", 1) + 1;
                        ((ObjectNode) item.json()).put("version", newVersion);
                    }
                    Document docToInsert = Document.parse(item.json().toString());
                    collection.insertOne(docToInsert);
                    logger.debug("Inserted UID {} with version {}", uid, item.json().get("version").asInt());
                }
            } catch (InterruptedException e) {
                logger.warn("DatabaseLinksWorker interrupted. Exiting ..");
                Thread.currentThread().interrupt();
            }
        };
        new Thread(writer, "FileWriterThread").start();
    }
}

