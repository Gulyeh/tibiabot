package apis;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


@Slf4j
public abstract class WebClient {
    // FlareSolverr response models
    private static class FlareSolverrResponse {
        Solution solution;
    }

    private static class Solution {
        String response;
    }

    private final OkHttpClient httpClient;

    public WebClient() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(150);
        dispatcher.setMaxRequestsPerHost(150);
        
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(20, 300, TimeUnit.SECONDS))
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build();
    }

    protected String sendRequest(Request request) {
        String responseBody = "";

        try (Response response = httpClient.newCall(request).execute()) {
            responseBody = response.body().string();
        } catch (IOException e) {
            log.info(e.getMessage());
        }

        return responseBody;
    }

    protected String sendRequestViaFlareSolverr(String url) {
        return sendRequestViaFlareSolverr(url, "http://localhost:8191/v1");
    }

    protected String sendRequestViaFlareSolverr(String url, String flareSolverrEndpoint) {
        String responseBody = "";

        try {
            String payload = String.format(
                "{\"cmd\":\"request.get\",\"url\":\"%s\",\"maxTimeout\":60000}",
                url
            );

            RequestBody body = RequestBody.create(
                payload,
                MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                .url(flareSolverrEndpoint)
                .post(body)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String jsonResponse = response.body().string();

                Gson gson = new Gson();
                FlareSolverrResponse flareResponse = gson.fromJson(jsonResponse, FlareSolverrResponse.class);

                if (flareResponse != null && flareResponse.solution != null) {
                    responseBody = flareResponse.solution.response;
                } else {
                    log.error("FlareSolverr failed to solve challenge");
                }
            }
        } catch (IOException e) {
            log.error("FlareSolverr request failed - {}", e.getMessage());
        }

        return responseBody;
    }

    protected abstract String getUrl();

    protected Request getCustomRequest(String url) {
        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }

    protected Request getRequest(String additionalParams) {
        return new Request.Builder()
                .url(getUrl() + additionalParams)
                .get()
                .build();
    }

    protected <T> T getModel(String response, Class<T> classType)
    {
        try {
            Gson g = new Gson();
            return g.fromJson(response, classType);
        } catch (Exception e) {
            log.info("Could not parse json data - {}", e.getMessage());
            return null;
        }
    }
}
