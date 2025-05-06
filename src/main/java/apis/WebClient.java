package apis;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


@Slf4j
public abstract class WebClient {
    private final OkHttpClient httpClient;

    public WebClient() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(20);
        dispatcher.setMaxRequestsPerHost(20);

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS))
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

    protected abstract String getUrl();

    protected Request getCustomRequest(String url) {
        return new Request.Builder().url(url).get().build();
    }

    protected Request getRequest(String additionalParams) {
        return new Request.Builder().url(getUrl() + additionalParams).get().build();
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
