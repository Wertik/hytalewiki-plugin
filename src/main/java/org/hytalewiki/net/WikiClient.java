package org.hytalewiki.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hytalewiki.net.response.PageObject;
import org.hytalewiki.net.response.SearchResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class WikiClient {

    private static final Logger log = Logger.getLogger(WikiClient.class.getName());

    private final String baseUrl;

    private final HttpClient.Builder builder;

    private HttpClient client;

    public WikiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20));

        this.rebuild();
    }

    public WikiClient(String baseUrl, HttpClient.Builder builder) {
        this.baseUrl = baseUrl;
        this.builder = builder;

        this.rebuild();
    }

    public static class PathBuilder {
        private String path;

        private final Map<String, String> params = new HashMap<>();

        PathBuilder(String path) {
            this.path = path;
        }

        public static PathBuilder create(String path) {
            return new PathBuilder(path);
        }

        public PathBuilder appendPath(String path) {
            this.path += path;
            return this;
        }

        public PathBuilder param(String key, String value) {
            params.put(key, value);
            return this;
        }

        public String buildQuery() {
            StringBuilder builder = new StringBuilder();
            if (!params.isEmpty()) {
                builder.append("?");
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    builder.append(entry.getKey()).append("=")
                            .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                            .append("&");
                }
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        }

        public URI toURI() {
            return URI.create(toString());
        }

        @Override
        public String toString() {
            return this.path.replace(" ", "_") + this.buildQuery();
        }
    }

    // Get the raw html for a page.
    public String html(String pageKey) throws RequestException {
        HttpRequest request = requestBase()
                .uri(buildRestPath("/page/" + pageKey + "/html")
                        .toURI())
                .build();

        try {
            HttpResponse<String> send = this.client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (send.statusCode() != 200) {
                throw new RequestException("Request failed: " + send.body());
            }

            return send.body();
        } catch (IOException | InterruptedException e) {
            throw new RequestException(e);
        }
    }

    // Get information about a page.
    public PageObject page(String key) throws RequestException {
        HttpRequest request = jsonRequest()
                .uri(buildRestPath("/page/" + key).toURI())
                .build();

        return sendJsonRequest(request, PageObject.class);
    }

    // Search for term on the wiki.
    public SearchResult search(String term, int limit) throws RequestException {
        HttpRequest request = buildSearchRequest(term, limit);
        return sendJsonRequest(request, SearchResult.class);
    }

    // Search for title on the wiki.
    public SearchResult searchTitle(String term, int limit) throws RequestException {
        HttpRequest request = buildSearchTitleRequest(term, limit);
        return sendJsonRequest(request, SearchResult.class);
    }

    public String getPageUrl(String term) {
        return this.baseUrl + "/w/" + term.replace(" ", "_");
    }

    public String getEditPageUrl(String key) {
        return this.buildPath("/w/" + key.replace(" ", "_"))
                .param("action", "edit")
                .toString();
    }

    public PathBuilder buildPath(String path) {
        return PathBuilder.create(this.baseUrl).appendPath(path);
    }

    public PathBuilder buildRestPath(String path) {
        return PathBuilder.create(this.baseUrl)
                .appendPath("/rest.php/v1")
                .appendPath(path);
    }

    public HttpRequest.Builder requestBase() {
        return HttpRequest.newBuilder()
                .timeout(Duration.ofMinutes(2))
                .GET();
    }

    public HttpRequest.Builder jsonRequest() {
        return requestBase()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .GET();
    }

    public <T> T sendJsonRequest(HttpRequest request, Class<T> clazz) throws RequestException {
        try {
            HttpResponse<InputStream> send = this.client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (Reader reader = new InputStreamReader(send.body(), StandardCharsets.UTF_8)) {
                Gson gson = new GsonBuilder().create();
                return gson.fromJson(reader, clazz);
            }
        } catch (IOException | InterruptedException e) {
            throw new RequestException(e);
        }
    }

    public HttpRequest buildSearchRequest(String term, int limit) {
        return jsonRequest()
                .uri(buildRestPath("/search/page")
                        .param("q", term)
                        .param("limit", String.valueOf(limit))
                        .toURI())
                .build();
    }

    public HttpRequest buildSearchTitleRequest(String term, int limit) {
        return jsonRequest()
                .uri(buildRestPath("/search/title")
                        .param("q", term)
                        .param("limit", String.valueOf(limit))
                        .toURI())
                .build();
    }


    public void rebuild() {
        this.client = this.builder.build();
    }

    public HttpClient.Builder builder() {
        return this.builder;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
