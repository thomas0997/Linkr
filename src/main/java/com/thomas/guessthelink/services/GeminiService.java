package com.thomas.guessthelink.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.thomas.guessthelink.GeneratedQuestion;
import com.thomas.guessthelink.repository.RejectedAnswerRepository;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Autowired
    private RejectedAnswerRepository rejectedAnswerRepo;  // ← ADD THIS

    private final HttpClient client = HttpClient.newHttpClient();

    public GeneratedQuestion generateQuestion() throws Exception {
        // ← NEW: build rejected list from DB
        List<String> rejected = rejectedAnswerRepo.findAll()
            .stream()
            .map(r -> r.getAnswer())
            .collect(Collectors.toList());

        String rejectedLine = rejected.isEmpty() ? "" :
            "- Do NOT use any of these already-rejected answers: " + String.join(", ", rejected) + "\n";

        // ← NEW: retry loop — tries up to 5 times to get a non-rejected answer
        for (int attempt = 0; attempt < 5; attempt++) {
            GeneratedQuestion q = callGroq(rejectedLine);
            if (!rejectedAnswerRepo.existsByAnswerIgnoreCase(q.getAnswer())) {
                return q;
            }
            System.out.println("Attempt " + (attempt + 1) + ": skipping rejected answer: " + q.getAnswer());
        }

        throw new RuntimeException("Could not generate a non-rejected question after 5 attempts.");
    }

    // ← Extracted into its own method so the retry loop can call it cleanly
    private GeneratedQuestion callGroq(String rejectedLine) throws Exception {
        String prompt =
            "Generate a Guess the Link puzzle where 3 images are connected by a hidden link through wordplay or cultural references.\n" +
            "Example: sole of shoe + brussels sprouts + deli meat shop = Capital Cities (Seoul, Brussels, Delhi)\n" +
            "Example: A fly, A lamp shade, a feather = Boxing Weight Classes (Flyweight, Lightweight, Featherweight)\n" +
            "Example: Bike Handle Bars, A Goat, Mutton Meat = Types of Mustaches (Handlebar, Goatee, Mutton Chops)\n" +
            "Example: A slice of bread, Flag of Switzerland, a drum set = Types of Rolls (Bread Roll, Swiss Roll, Drum Roll)\n" +
            "Example: A picture of a bicycle, a grass field with a rainbow, and a scorpion = Football Skills (Bicycle Kick, Rainbow Flick, Scorpion Kick)\n" +
            "Example: DNA strand, A flare being lit up, and a Cargo Ship with containers = Types of Trousers (Jeans, Flares, Cargo)\n" +
            "Example: A man playing a guitar, optimus prime in transformers, and the movie Monsters Inc = Energy Drinks (Rockstar, Prime, Monsters)\n" +
            "Example: A Whale swimming, The Giga Chad Meme, and picture of germs = Countries (Wales, Chad, Germany)\n" +
            "Example: Grass, A computer program, and a corn = Types of Snakes (Grass Snake, Python, Corn Snake)\n" +
            "Example: A bat, Frank Sinatra (The Singer), and Raheem Sterling (The Footballer) = Currencies (Baht, Franc, Sterling)\n" +
            "Rules:\n" +
            "- The answer must be a common English phrase, category, or concept.\n" +
            "- Its a riddle-like challenge. Don't Make it Obvious and make it tricky.\n" +
            "- Each of the 3 images must connect to the answer through wordplay (homophones, sounds-like, compound words).\n" +
            "- The images should be visually clear and not too obscure.\n" +
            "- Do NOT reuse any of the examples above.\n" +
            "- The wordplay must work in English.\n" +
            "- Provide 3 clues that get progressively easier. The last clue should almost give it away.\n" +
            rejectedLine +   // ← injected here
            "Return ONLY valid JSON, no markdown, no code fences, no explanation. Exactly this structure:\n" +
            "{\"answer\":\"the link\",\"imageKeyword1\":\"search term for image 1\",\"imageKeyword2\":\"search term for image 2\",\"imageKeyword3\":\"search term for image 3\",\"clue1\":\"vague category hint\",\"clue2\":\"narrows it down\",\"clue3\":\"almost gives it away\"}";

        String requestBody =
            "{\"model\":\"llama-3.3-70b-versatile\"," +
            "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeForJson(prompt) + "\"}]}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResponse(response.body());
    }

    private GeneratedQuestion parseResponse(String rawResponse) {
        String content = extractJsonString(rawResponse, "content");

        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Groq returned an unparseable response.");
        }

        content = content.replace("```json", "").replace("```", "").trim();

        String answer   = extractJsonString(content, "answer");
        String keyword1 = extractJsonString(content, "imageKeyword1");
        String keyword2 = extractJsonString(content, "imageKeyword2");
        String keyword3 = extractJsonString(content, "imageKeyword3");
        String clue1    = extractJsonString(content, "clue1");
        String clue2    = extractJsonString(content, "clue2");
        String clue3    = extractJsonString(content, "clue3");

        if (answer == null || answer.isEmpty()) {
            throw new RuntimeException("Groq JSON missing 'answer' field. Content: " + content);
        }

        return new GeneratedQuestion(answer, "", "", "", keyword1, keyword2, keyword3, clue1, clue2, clue3);
    }

    private String escapeForJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String extractJsonString(String json, String field) {
        String key = "\"" + field + "\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx == -1) return "";
        int colon = json.indexOf(":", keyIdx + key.length());
        if (colon == -1) return "";
        int pos = colon + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        if (pos >= json.length() || json.charAt(pos) != '"') return "";
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == '\\' && pos + 1 < json.length()) {
                char next = json.charAt(pos + 1);
                switch (next) {
                    case '"':  sb.append('"');  pos += 2; continue;
                    case '\\': sb.append('\\'); pos += 2; continue;
                    case 'n':  sb.append('\n'); pos += 2; continue;
                    case 'r':  sb.append('\r'); pos += 2; continue;
                    case 't':  sb.append('\t'); pos += 2; continue;
                    default:   sb.append(c);    pos++;    continue;
                }
            }
            if (c == '"') break;
            sb.append(c);
            pos++;
        }
        return sb.toString();
    }
}