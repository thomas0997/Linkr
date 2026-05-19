package com.thomas.guessthelink.controller;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.thomas.guessthelink.*;
import com.thomas.guessthelink.services.*;
import com.thomas.guessthelink.security.TotpService;
import com.thomas.guessthelink.repository.*;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class AdminController {

    @Autowired GeminiService geminiService;
    @Autowired UnsplashService unsplashService;
    @Autowired QuestionRepository questionRepo;
    @Autowired RejectedAnswerRepository rejectedAnswerRepo;
    @Autowired TotpService totpService;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.totp.secret}")
    private String totpSecret;

    // ── Login page
    @GetMapping("/admin/login")
    public String showAdminLogin() {
        return "admin-login";
    }

    // ── Login submit — requires BOTH password and OTP code
    @PostMapping("/admin/login")
    public String handleAdminLogin(@RequestParam String password,
                                   @RequestParam String code,
                                   HttpSession session,
                                   Model model) {
        if (!adminPassword.equals(password)) {
            model.addAttribute("error", "Wrong password.");
            return "admin-login";
        }
        if (!totpService.verify(totpSecret, code)) {
            model.addAttribute("error", "Wrong or expired code. Open Google Authenticator and try again.");
            return "admin-login";
        }
        session.setAttribute("adminLoggedIn", true);
        return "redirect:/admin";
    }

    // ── Setup page — shows QR code to scan into Google Authenticator
    @GetMapping("/admin/setup")
    public String showSetup(Model model) {
        String qrUrl = totpService.getQRCodeImageUrl(totpSecret, "admin", "Linkr");
        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("secret", totpSecret);
        return "admin-setup";
    }

    // ── Admin panel
    @GetMapping("/admin")
    public String showAdmin(HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute("adminLoggedIn"))) {
            return "redirect:/admin/login";
        }
        int nextLevel = questionRepo.findAll()
            .stream()
            .mapToInt(q -> q.getLevelNumber())
            .max()
            .orElse(0) + 1;
        model.addAttribute("nextLevel", nextLevel);
        model.addAttribute("message", "Welcome. Next suggested level: " + nextLevel);
        return "admin";
    }

    // ── Logout
    @GetMapping("/admin/logout")
    public String adminLogout(HttpSession session) {
        session.removeAttribute("adminLoggedIn");
        return "redirect:/admin/login";
    }

    // ── Generate
    @PostMapping("/admin/generate")
    public String generateQuestion(Model model) {
        if (model.getAttribute("nextLevel") == null) {
            int nextLevel = questionRepo.findAll()
                .stream()
                .mapToInt(q -> q.getLevelNumber())
                .max()
                .orElse(0) + 1;
            model.addAttribute("nextLevel", nextLevel);
        }
        try {
            GeneratedQuestion q = geminiService.generateQuestion();
            q.setImageUrl1(unsplashService.getImageUrl(q.getImageKeyword1()));
            q.setImageUrl2(unsplashService.getImageUrl(q.getImageKeyword2()));
            q.setImageUrl3(unsplashService.getImageUrl(q.getImageKeyword3()));
            model.addAttribute("generated", q);
            model.addAttribute("message", "Review the question below.");
        } catch (Exception e) {
            model.addAttribute("error", "Generation failed: " + e.getMessage());
        }
        return "admin";
    }

    // ── Upload image
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

    // ── Refetch image
    @GetMapping("/admin/refetch-image")
    @ResponseBody
    public Map<String, Object> refetchImage(@RequestParam String keyword) {
        try {
            String url = unsplashService.getImageUrl(keyword);
            if (url == null || url.isBlank()) return Map.of("error", "No image found for: " + keyword);
            return Map.of("url", url);
        } catch (Exception e) {
            return Map.of("error", "Unsplash error: " + e.getMessage());
        }
    }

    // ── Approve
    @PostMapping("/admin/approve")
    public String approveQuestion(
        @RequestParam String answer,
        @RequestParam String imageUrl1, @RequestParam String imageUrl2, @RequestParam String imageUrl3,
        @RequestParam String clue1, @RequestParam String clue2, @RequestParam String clue3,
        @RequestParam int levelNumber, Model model) {
        questionRepo.save(new Question(clue1, clue2, clue3, imageUrl1, imageUrl2, imageUrl3, answer, levelNumber));
        model.addAttribute("message", "✓ Question saved for Level " + levelNumber);
        return "admin";
    }

    // ── Reject
    @PostMapping("/admin/reject")
    public String rejectQuestion(@RequestParam(required = false) String answer, Model model) {
        if (answer != null && !answer.isBlank()) rejectedAnswerRepo.save(new RejectedAnswer(answer));
        return generateQuestion(model);
    }

    // ── Regenerate
    @PostMapping("/admin/regenerate")
    public String regenerateQuestion(@RequestParam(required = false) String answer, Model model) {
        if (answer != null && !answer.isBlank()) rejectedAnswerRepo.save(new RejectedAnswer(answer));
        return generateQuestion(model);
    }
    // ── TEMPORARY DEBUG — remove after fixing
@GetMapping("/admin/totp-test")
@ResponseBody
public String totpTest() {
    long window = java.time.Instant.now().getEpochSecond() / 30;
    try {
        byte[] secret = totpService.base32Decode(totpSecret);
        StringBuilder sb = new StringBuilder();
        sb.append("Secret in use: ").append(totpSecret).append("\n");
        sb.append("Current window: ").append(window).append("\n");
        // call verify with a dummy to trigger the console print
        totpService.verify(totpSecret, "000000");
        sb.append("Check your console — it printed the expected codes for windows ")
          .append(window - 1).append(", ").append(window).append(", ").append(window + 1);
        return sb.toString();
    } catch (Exception e) {
        return "ERROR: " + e.getMessage();
    }
    }
}
