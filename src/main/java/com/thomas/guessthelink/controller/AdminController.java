package com.thomas.guessthelink.controller;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.thomas.guessthelink.*;
import com.thomas.guessthelink.services.*;
import com.thomas.guessthelink.repository.*;
import java.util.Map;


@Controller
public class AdminController {

    @Autowired GeminiService geminiService;
    @Autowired UnsplashService unsplashService;
    @Autowired QuestionRepository questionRepo;
    @Autowired RejectedAnswerRepository rejectedAnswerRepo;  // ← ADD THIS

    @GetMapping("/admin")
    public String showAdmin(Model model) {
        model.addAttribute("message", "Welcome to the admin panel.");
        return "admin";
    }

    @PostMapping("/admin/generate")
    public String generateQuestion(Model model) {
        try {
            GeneratedQuestion q = geminiService.generateQuestion();

            String img1 = unsplashService.getImageUrl(q.getImageKeyword1());
            String img2 = unsplashService.getImageUrl(q.getImageKeyword2());
            String img3 = unsplashService.getImageUrl(q.getImageKeyword3());

            q.setImageUrl1(img1);
            q.setImageUrl2(img2);
            q.setImageUrl3(img3);

            model.addAttribute("generated", q);
            model.addAttribute("message", "Review the question below.");

        } catch (Exception e) {
            model.addAttribute("error", "Generation failed: " + e.getMessage());
        }
        return "admin";
    }

    @PostMapping("/admin/upload-image")
    @ResponseBody
    public Map<String, Object> uploadImage(@RequestParam MultipartFile file) {
        try {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path uploadDir = Paths.get("src/main/resources/static/uploads");
            Files.createDirectories(uploadDir);
            Files.write(uploadDir.resolve(filename), file.getBytes());
            return Map.of("url", "/uploads/" + filename);
        } catch (Exception e) {
            return Map.of("error", "Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/admin/approve")
    public String approveQuestion(
        @RequestParam String answer,
        @RequestParam String imageUrl1,
        @RequestParam String imageUrl2,
        @RequestParam String imageUrl3,
        @RequestParam String clue1,
        @RequestParam String clue2,
        @RequestParam String clue3,
        @RequestParam int levelNumber,
        Model model
    ) {
        Question q = new Question(clue1, clue2, clue3, imageUrl1, imageUrl2, imageUrl3, answer, levelNumber);
        questionRepo.save(q);
        model.addAttribute("message", "✓ Question saved for Level " + levelNumber);
        return "admin";
    }
    // Reject — saves answer, then immediately regenerates
    @PostMapping("/admin/reject")
    public String rejectQuestion(@RequestParam(required = false) String answer, Model model) {
        if (answer != null && !answer.isBlank()) {
            rejectedAnswerRepo.save(new RejectedAnswer(answer));
            System.out.println("Rejected: " + answer);
        }
        return generateQuestion(model); // ← auto-regenerates instead of going to empty panel
    }

    // Regenerate — also counts as a reject now, then generates
    @PostMapping("/admin/regenerate")
    public String regenerateQuestion(@RequestParam(required = false) String answer, Model model) {
        if (answer != null && !answer.isBlank()) {
            rejectedAnswerRepo.save(new RejectedAnswer(answer));
            System.out.println("Regenerate-rejected: " + answer);
        }
        return generateQuestion(model);
    }
}