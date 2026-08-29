package apis;

import apis.proxy.flareSolver.models.FlaresolverModel;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static apis.proxy.flareSolver.FlareSolver.sendRequestViaFlareSolver;


@Slf4j
public abstract class WebClient {
    private final OkHttpClient httpClient;

    public WebClient() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(200);
        dispatcher.setMaxRequestsPerHost(150);
        
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(120, 300, TimeUnit.SECONDS))
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(false)
                .build();
    }

    protected String sendRequest(Request request) {
        String responseBody = "";

        try (Response response = httpClient.newCall(request).execute()) {
            logErrorResponse(response);
            responseBody = response.body().string();
        } catch (IOException e) {
            log.info(e.getMessage());
        }

        return responseBody;
    }

    protected byte[] sendRequestWithByteResponse(Request request) {
        try (Response response = httpClient.newCall(request).execute()) {
            logErrorResponse(response);
            if(response.isSuccessful())
                return response.body().bytes();
            return null;
        } catch (IOException e) {
            log.info(e.getMessage());
        }

        return null;
    }

    protected FlaresolverModel sendRequestUsingFlareSolver(String url) {
        return sendRequestViaFlareSolver(httpClient, url);
    }

    protected abstract String getUrl();

    protected Request getCustomRequest(String url) {
        return new Request.Builder()
                .header("User-Agent", UUID.randomUUID().toString())
                .url(url)
                .get()
                .build();
    }

    protected Request getCustomRequest(FlaresolverModel.Solution flameSolution) {
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
                .url(flameSolution.getUrl())
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

    protected <T> T getModel(String response, Type type)
    {
        try {
            Gson g = new Gson();
            return g.fromJson(response, type);
        } catch (Exception e) {
            return null;
        }
    }

    @SneakyThrows
    private void logErrorResponse(Response response) {
        if(response.isSuccessful()) return;
        if(response.code() == 429)
            log.info("Request to {} had Rate Limit", response.request().url());
        else if(response.code() == 502)
            log.info("Request to {} was unreachable", response.request().url());
        else {
            log.info("Error Code - {}", response.code());
            log.info("Error Body - {}", response.body().string());
        }
    }
}
