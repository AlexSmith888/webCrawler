package server.requests;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import server.requests.*;
import server.raw.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AppHtmlResponse {
    /*
    Sends a request, parses and returns a document body
    */
    public static Document returnDoc(RawQueueItem item, int httpResponseTimeOut)
            throws InterruptedException, IOException {
        HttpClient client = AppHttpClient.INSTANCE.getClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(item.message()))
                .timeout(Duration.ofSeconds(httpResponseTimeOut))
                .build();
        HttpResponse<String> resp
                = client.send(request, HttpResponse.BodyHandlers.ofString());
        return Jsoup.parse(resp.body(), item.message());
    }
}
