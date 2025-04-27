package apis;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;


@Slf4j
public abstract class WebClient {
    private final CloseableHttpClient httpClient;

    public WebClient() {
        httpClient = HttpClients.createDefault();
    }

    protected String sendRequest(ClassicHttpRequest request) {
        BasicHttpClientResponseHandler responseHandler = new BasicHttpClientResponseHandler();
        String response = "";

        try {
             response = httpClient.execute(request, responseHandler);
        } catch (Exception e) {
            log.info(e.getMessage());
        }

        return response;
    }

    protected abstract String getUrl();

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
            log.info("Could not parse json data - {}", e.getMessage());
            return null;
        }
    }
}
