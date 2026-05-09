package com.thomas.guessthelink.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.thomas.guessthelink.GeneratedQuestion;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final HttpClient client = HttpClient.newHttpClient();

    public GeneratedQuestion generateQuestion() throws Exception {

        String prompt = """
            Generate a "Guess the Link" puzzle where 3 images are connected by a hidden link through wordplay or cultural references.
            Example: sole of shoe + brussels sprouts + deli meat shop = Capital Cities (Seoul, Brussels, Delhi)
            The wordplay must work in English.
            Return ONLY valid JSON, no markdown, no extra text:
            {
              "answer": "the link",
              "imageKeyword1": "specific search term for image 1",
              "imageKeyword2": "specific search term for image 2",
              "imageKeyword3": "specific search term for image 3",
              "reasoning1": "why image 1 connects to the answer",
              "reasoning2": "why image 2 connects to the answer",
              "reasoning3": "why image 3 connects to the answer",
              "clue1": "vague category hint",
              "clue2": "narrows it down",
              "clue3": "almost gives it away"
            }
        """;

        String requestBody = """
            {
              "contents": [{
                "parts": [{
                  "text": "%s"
                }]
              }]
            }
        """.formatted(prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl + "?key=" + apiKey))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return parseResponse(response.body());
    }

    private GeneratedQuestion parseResponse(String json) {
        // extract the text content from Gemini response
        String text = extractField(json, "text");

        // clean up any markdown formatting Gemini might add
        text = text.replace("```json", "").replace("```", "").trim();

        String answer = extractField(text, "answer");
        String keyword1 = extractField(text, "imageKeyword1");
        String keyword2 = extractField(text, "imageKeyword2");
        String keyword3 = extractField(text, "imageKeyword3");
        String clue1 = extractField(text, "clue1");
        String clue2 = extractField(text, "clue2");
        String clue3 = extractField(text, "clue3");

        GeneratedQuestion q = new GeneratedQuestion(
            answer, "", "", "", keyword1, keyword2, keyword3, clue1, clue2, clue3
        );
        return q;
    }

    // simple JSON field extractor — no library needed
    private String extractField(String json, String field) {
        String search = "\"" + field + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        int colon = json.indexOf(":", idx);
        int quote1 = json.indexOf("\"", colon);
        int quote2 = json.indexOf("\"", quote1 + 1);
        if (quote1 == -1 || quote2 == -1) return "";
        return json.substring(quote1 + 1, quote2);
    }
}