package server.utils;

import org.apache.logging.log4j.Logger;
import server.requests.AppHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ValidationChecks {
    /*
    Checks if a link is available by sending a request that does not contain a response body
    if error happens - false
    if a server error or resource is unavailable - false
    else true
    */
    static public boolean linkIsValid(String link, int httpResponseTimeOut, Logger logger){
        if (!link.startsWith("http")) {
            return false;
        }
        int code = 500;
        try {
            HttpClient client = AppHttpClient.INSTANCE.getClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(link))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(httpResponseTimeOut))
                    .build();
            HttpResponse<Void> response = client.send(request
                    , HttpResponse.BodyHandlers.discarding());
            code = response.statusCode();
            return isResponseValid(code);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            logger.warn("There is an error " +
                    "while requesting the link {}: code {}: ", link, code);
            return false;
        }
    }
    static public boolean isResponseValid(int status){
        return status >= 200 && status < 400;
    }

    static public boolean linkIsSuccessor(String parent, String child) {
        return child.startsWith(parent);
    }

}
