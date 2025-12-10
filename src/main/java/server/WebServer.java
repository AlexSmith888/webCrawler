package server;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.database.DatabaseLinksWorker;
import server.database.DbQueueItem;
import server.raw.Initialization;
import server.raw.RawLinksWorker;
import server.raw.RawQueueItem;
import server.transformation.TranformLinksWorker;
import server.utils.AppLaunch;
import server.transformation.TransformQueueItem;
import server.utils.ProjectVariables;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.*;

public class WebServer {
    /*
    Webserver class :
    - Initializes the projects by receiving main parameters as an input.
    - creates thread pools
        There are three thread pools for different purposes:
            RawPool manages the rawQueue that contains raw links
            ValidPool manages the validQueue, which contains filtered valid links
            DatabasePool manages the DatabaseQueue that contains links prepared for a load.
    - manages a correct exit (addShutdownHook), closing all resources and threads
    - launches all workers (RawLinksWorker, ValidLinksWorker, DatabaseLinksWorker)
    - launches a Server Socket that listens for incoming requests
    - Thread - safe (concurrent utils are used like nonblocking queues and hashmaps)
    **/
    static int NUM_OF_THREADS = Runtime.getRuntime().availableProcessors(); //number of threads(workers) per thread pool, default — number of cores
    static int DEPTH = 3;//how many steps from a parent link to a child is supposed to be done
    static int HTTP_RESPONSE_TIME_OUT = 10;//for how long to wait for a response
    static int MAX_QUEUE_SIZE = 10000;//limits a value to avoid memory leaks
    private static final Logger logger = LogManager.getLogger(WebServer.class);
    static MongoDatabase DB;
    static MongoClient mongoclient;

    public static Properties customProperties = new Properties();
    static {
        try (var file = WebServer.class.getResourceAsStream("/config.properties")) {
            customProperties.load(file);
        } catch (IOException e) {
            logger.warn("ProjectVariables File does not exists");
            throw new RuntimeException(e);
        }

        StringBuilder uri = new StringBuilder("mongodb://");
        uri.append(customProperties.getProperty(ProjectVariables.DB_HOST.label));
        uri.append(":");
        uri.append(customProperties.getProperty(ProjectVariables.DB_PORT.label));

        mongoclient = MongoClients.create(uri.toString());
        DB = mongoclient.getDatabase(customProperties
                    .getProperty(ProjectVariables.DB_NAME.label));
    }

    public static void main(String[] args){

        if (args.length == 0) {
            logger.error("The port number Should be strictly provided");
            throw new IllegalStateException("Port should be provided");
        }

        NUM_OF_THREADS = args.length < 2 ? NUM_OF_THREADS : Integer.parseInt(args[1]);
        DEPTH = args.length < 3 ? DEPTH : Integer.parseInt(args[2]);
        HTTP_RESPONSE_TIME_OUT = args.length < 4 ? HTTP_RESPONSE_TIME_OUT : Integer.parseInt(args[3]);
        MAX_QUEUE_SIZE = args.length < 5 ? MAX_QUEUE_SIZE : Integer.parseInt(args[4]);
        AppLaunch.start(logger, NUM_OF_THREADS, DEPTH, HTTP_RESPONSE_TIME_OUT, MAX_QUEUE_SIZE);

        BlockingQueue<RawQueueItem> rawLinksQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        BlockingQueue<TransformQueueItem> validLinksQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        BlockingQueue<DbQueueItem> databaseQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        Set<String> seen = ConcurrentHashMap.newKeySet();

        ExecutorService mainPool = Executors.newFixedThreadPool(NUM_OF_THREADS);
        ExecutorService rawPool = Executors.newFixedThreadPool(NUM_OF_THREADS);
        ExecutorService validLinksPool = Executors.newFixedThreadPool(NUM_OF_THREADS);
        //ExecutorService databasePool = Executors.newFixedThreadPool(NUM_OF_THREADS);

        new RawLinksWorker().processRawQueue(rawLinksQueue, validLinksQueue, seen, rawPool,
                NUM_OF_THREADS, DEPTH, logger, HTTP_RESPONSE_TIME_OUT);
        new TranformLinksWorker().transformRawQueueItem(validLinksQueue, databaseQueue, validLinksPool,
                NUM_OF_THREADS, logger);
        new DatabaseLinksWorker().savePreparedItem(databaseQueue, logger, seen, DB);

        //starts server socket
        ServerSocket server;
        try {
            server = new ServerSocket(Integer.parseInt(args[0]));
        } catch (IOException e) {
            logger.error("Could not start server");
            return;
        }

        //exit from the application, closing all server's resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown initiated...");
            try {
                server.close();
            } catch (IOException e) {
                logger.warn("Failed to close server socket.");
            }

            mongoclient.close();

            rawPool.shutdownNow();
            validLinksPool.shutdownNow();
            mainPool.shutdownNow();

            try {
                rawPool.awaitTermination(5, TimeUnit.SECONDS);
                validLinksPool.awaitTermination(5, TimeUnit.SECONDS);
                mainPool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            AppLaunch.stop(logger);
        }));

        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = server.accept();
                //instantiates the mandatory class
                mainPool.execute(new Initialization(client, rawLinksQueue, HTTP_RESPONSE_TIME_OUT, logger));
            }
        } catch (IOException e) {
            logger.error("Server loop terminated.");
        }
    }
}
