package apis;

import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class WebClient {
    private final HttpClient httpClient;
    protected Logger logINFO = LoggerFactory.getLogger(WebClient.class);

    public WebClient() {
        httpClient = HttpClients.createDefault();
    }

    protected String sendRequest(ClassicHttpRequest request) {
        BasicHttpClientResponseHandler responseHandler = new BasicHttpClientResponseHandler();
        String response = "";

        try {
             response = httpClient.execute(request, responseHandler);
        } catch (Exception e) {
            logINFO.info(e.getMessage());
        }

        return response;
    }

    protected abstract String getUrl();

    protected ClassicHttpRequest getRequest() {
        return new HttpGet(getUrl());
    }

    protected ClassicHttpRequest getCustomRequest(String url) {
        return new HttpGet(url);
    }

    protected ClassicHttpRequest getRequest(String additionalParams) {
        return new HttpGet(getUrl() + additionalParams);
    }

    protected <T> T getModel(String response, Class<T> classType)
    {
        try {
            Gson g = new Gson();
            return g.fromJson(response, classType);
        } catch (Exception e) {
            logINFO.info("Could not parse json data - " + e.getMessage());
            return null;
        }
    }
}
