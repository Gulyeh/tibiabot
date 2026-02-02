package apis;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Slf4j
public abstract class WebClient {
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

    protected byte[] sendRequestWithByteResponse(Request request) {
        try (Response response = httpClient.newCall(request).execute()) {
            return response.body().bytes();
        } catch (IOException e) {
            log.info(e.getMessage());
        }

        return null;
    }

    protected FlaresolverModel sendRequestViaFlareSolverr(String url) {
        return sendRequestViaFlareSolverr(url, "http://localhost:8191/v1");
    }

    protected FlaresolverModel sendRequestViaFlareSolverr(String url, String flareSolverrEndpoint) {
        FlaresolverModel responseBody;

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
                responseBody = gson.fromJson(jsonResponse, FlaresolverModel.class);

                if (responseBody != null && responseBody.getSolution() != null) {
                    return responseBody;
                } else
                    log.error("FlareSolverr failed to solve challenge");
            }
        } catch (IOException e) {
            log.error("FlareSolverr request failed - {}", e.getMessage());
        }

        return null;
    }

    protected abstract String getUrl();

    protected Request getCustomRequest(String url) {
        return new Request.Builder()
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36")
                .url(url)
                .get()
                .build();
    }

    protected Request getCustomRequest(String url, FlaresolverModel.Solution flameSolution) {
        StringBuilder cookieHeader = new StringBuilder();
        for (Map<String, Object> map : flameSolution.getCookies()) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                cookieHeader.append(entry.getKey())
                        .append("=")
                        .append(entry.getValue())
                        .append("; ");
            }
        }
        return new Request.Builder()
                .header("User-Agent", flameSolution.getUserAgent())
                .header("Cookie", cookieHeader.toString())
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
            return null;
        }
    }
}
