package server.requests;

import java.net.http.HttpClient;

public enum AppHttpClient {
    /*
    The application is required to have only one version of the HTTP client
    Therefore, the singleton pattern is used to instantiate a unique instance of the class
    */
    INSTANCE;
    private final HttpClient client;
    AppHttpClient(){
        client = HttpClient
                .newBuilder().followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }
    public HttpClient getClient (){
        return client;
    }
}
