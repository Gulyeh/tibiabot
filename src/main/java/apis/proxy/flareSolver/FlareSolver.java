package apis.proxy.flareSolver;

import apis.proxy.flareSolver.models.FlaresolverModel;
import apis.proxy.flareSolver.models.FlaresolverSession;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

@Slf4j
public final class FlareSolver {
    private FlareSolver() {}

    public static String flareSolverAddress = "http://localhost:8191/v1";

    public static FlaresolverModel sendRequestViaFlareSolver(OkHttpClient httpClient, String url) {
        String payload = String.format(
                "{\"cmd\": \"request.get\",\"url\": \"%s\",\"maxTimeout\": 60000}",
                url
        );

        RequestBody body = RequestBody.create(
                payload,
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(flareSolverAddress)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String jsonResponse = response.body().string();

            Gson gson = new Gson();
            FlaresolverModel responseBody = gson.fromJson(jsonResponse, FlaresolverModel.class);

            if (responseBody != null && responseBody.getSolution() != null)
                return responseBody;

            log.error("FlareSolverr failed to solve challenge");
        } catch (Exception e) {
            log.error("FlareSolverr request failed - {}", e.getMessage());
        }

        return null;
    }

    private static String createSession(OkHttpClient httpClient) {
        log.info("Creating FlareSolver Session");

        RequestBody body = RequestBody.create(
                "{\"cmd\": \"sessions.create\"}",
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(flareSolverAddress)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String jsonResponse = response.body().string();

            Gson gson = new Gson();
            FlaresolverSession responseBody = gson.fromJson(jsonResponse, FlaresolverSession.class);

            if (responseBody != null && !responseBody.getSession().isEmpty()) {
                log.info("Created Flaresolver Session");
                return responseBody.getSession();
            } else
                log.error("FlareSolverr failed to create session");
        } catch (Exception e) {
            log.error("FlareSolverr session creation request failed - {}", e.getMessage());
        }

        return null;
    }

    private static void destroySession(OkHttpClient httpClient, String sessionId) {
        log.info("Destroying Flaresolver Session");

        RequestBody body = RequestBody.create(
                String.format("{\"cmd\": \"sessions.destroy\", \"session\": \"%s\"}", sessionId),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(flareSolverAddress)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful())
                log.info("Destroyed Flaresolver Session");
            else
                log.error("FlareSolverr failed to destroy session");
        } catch (Exception e) {
            log.error("FlareSolverr session destroy request failed - {}", e.getMessage());
        }
    }
}
