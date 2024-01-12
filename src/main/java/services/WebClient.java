package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public abstract class WebClient {
    private final HttpClient httpClient;
    protected final static Logger logINFO = LoggerFactory.getLogger(WebClient.class);

    public WebClient() {
        httpClient = HttpClient.newHttpClient();
    }

    protected HttpResponse<String> sendRequest(HttpRequest request) {
        HttpResponse<String> response = null;

        try {
             response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            logINFO.info(e.getMessage());
        }

        return response;
    }

    protected abstract String getUrl();

    protected HttpRequest getRequest() {
        return HttpRequest
                .newBuilder()
                .uri(getURI())
                .GET()
                .build();
    }

    protected <T> T getModel(HttpResponse<String> response, Class<T> classType)
    {
        try {
            Gson g = new Gson();
            return g.fromJson(response.body(), classType);
        } catch (Exception e) {
            logINFO.info("Could not parse json data - " + e.getMessage());
            return null;
        }
    }

    private URI getURI() {
        URI uri;

        try {
            uri = new URI(getUrl());
        } catch (Exception ignore) {
            try {
                logINFO.info("Could not parse URI - trying by URL");
                uri = new URL(getUrl()).toURI();
            } catch (Exception ignore2)
            {
                logINFO.info("Could not parse URL to URI");
                return null;
            }
        }

        return uri;
    }
}
