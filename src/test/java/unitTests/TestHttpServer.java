package unitTests;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class TestHttpServer {

    private HttpServer server;

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8081), 0);

        server.createContext("/index.html", serveFile("fakeUrls/index.html"));
        server.createContext("/depth1.html", serveFile("fakeUrls/depth1.html"));
        server.createContext("/depth2.html", serveFile("fakeUrls/depth2.html"));

        server.setExecutor(null);
        server.start();
    }

    private HttpHandler serveFile(String resourcePath) {
        return exchange -> {
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] bytes = is.readAllBytes();
            boolean isHead = "HEAD".equalsIgnoreCase(exchange.getRequestMethod());

            exchange.getResponseHeaders().add("Content-Type", "text/html");

            if (isHead) {
                // Send headers only, no body, no content length
                exchange.sendResponseHeaders(200, -1);
            } else {
                // Send full body for GET
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        };
    }


    public void stop() {
        server.stop(0);
    }
}
