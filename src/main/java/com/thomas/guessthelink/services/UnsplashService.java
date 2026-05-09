package com.thomas.guessthelink.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

@Service
public class UnsplashService {

    @Value("${unsplash.api.key}")
    private String apiKey;

    @Value("${unsplash.api.url}")
    private String apiUrl;

    private final HttpClient client = HttpClient.newHttpClient();

    public String getImageUrl(String keyword) throws Exception {
        String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = apiUrl + "?query=" + encoded + "&per_page=1";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Client-ID " + apiKey)
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return parseImageUrl(response.body());
    }

    private String parseImageUrl(String json) {
        // extract results[0].urls.regular
        String resultsMarker = "\"results\"";
        int resultsIdx = json.indexOf(resultsMarker);
        if (resultsIdx == -1) return "";

        String regularMarker = "\"regular\"";
        int regularIdx = json.indexOf(regularMarker, resultsIdx);
        if (regularIdx == -1) return "";

        int colon = json.indexOf(":", regularIdx);
        int quote1 = json.indexOf("\"", colon);
        int quote2 = json.indexOf("\"", quote1 + 1);
        if (quote1 == -1 || quote2 == -1) return "";

        return json.substring(quote1 + 1, quote2);
    }
}