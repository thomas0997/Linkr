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
/*
    public GeneratedQuestion generateQuestion() throws Exception {

        String prompt = """
            Generate a "Guess the Link" puzzle where 3 images are connected by a hidden link through wordplay or cultural references.
            Example: sole of shoe + brussels sprouts + deli meat shop = Capital Cities (Seoul, Brussels, Delhi)
            Another Example is: A fly, A lamp shade, and a feather. = Boxing Weight Classes (Flyweight, LightWeight, Featherweight)
            Another Example is: A man drinking alcohol, A picture of James Bond, and Jimmy Saville = Famous Streets in London (Bond Street, Saville Row, etc)
            Another Example is: Bike Handle Bars, A Goat, and Mutton Meat = Types of Mustaches (Handlebar, Goatee, Mutton Chops)
            Final Example is: A slice of bread, Flag of Switzdrland, and a drum set = Types of Rolls (Bread Roll, Swiss Roll, Drum Roll)
            The answer should be a common phrase, category, or concept that can be linked to all 3 images through wordplay or cultural references. 
            The images themselves should be fairly straightforward and not too obscure. 
            Provide 3 clues that get progressively easier but do not outright give away the answer until the last clue.
            The response must be in the following JSON format. 
            The "reasoning" fields should explain how each image connects to the answer in a way that would make sense to a human trying to solve the puzzle.
            Don't give out these same answers as a generated qeustion, these are just examples to show the format and style of the puzzles I want:
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
    }*/


        public GeneratedQuestion generateQuestion() throws Exception {
    String answer = "types of rolls";
    String k1 = "bread loaf bakery";
    String k2 = "switzerland flag";
    String k3 = "drum kit";
    String c1 = "Think: something that follows a word";
    String c2 = "Each image sounds like it precedes the same word";
    String c3 = "Bread ___, Swiss ___, Drum ___";

    GeneratedQuestion q = new GeneratedQuestion(
        answer, "", "", "", k1, k2, k3, c1, c2, c3
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