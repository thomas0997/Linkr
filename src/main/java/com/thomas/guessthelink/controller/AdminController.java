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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class AdminController {

    @Autowired GeminiService geminiService;
    @Autowired UnsplashService unsplashService;
    @Autowired QuestionRepository questionRepo;
    @Autowired RejectedAnswerRepository rejectedAnswerRepo;
    @Autowired TotpService totpService;
    @Autowired SessionTracker sessionTracker;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.totp.secret}")
    private String totpSecret;

    // ── Helper ────────────────────────────────────────────────────────────────

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("adminLoggedIn"));
    }

    // ── Login page ────────────────────────────────────────────────────────────

    @GetMapping("/admin/login")
    public String showAdminLogin() {
        return "admin-login";
    }

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
            model.addAttribute("error", "Wrong or expired code. Open your authenticator and try again.");
            return "admin-login";
        }
        session.setAttribute("adminLoggedIn", true);
        return "redirect:/admin";
    }

    // ── Setup page ────────────────────────────────────────────────────────────

    @GetMapping("/admin/setup")
    public String showSetup(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        String qrUrl = totpService.getQRCodeImageUrl(totpSecret, "admin", "Linkr");
        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("secret", totpSecret);
        return "admin-setup";
    }

    // ── Admin panel ───────────────────────────────────────────────────────────

    @GetMapping("/admin")
    public String showAdmin(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        int nextLevel = questionRepo.findAll()
            .stream().mapToInt(q -> q.getLevelNumber()).max().orElse(0) + 1;

        model.addAttribute("nextLevel", nextLevel);
        model.addAttribute("message", "Welcome. Next suggested level: " + nextLevel);
        model.addAttribute("activePlayers", sessionTracker.getActiveCount());
        return "admin";
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @GetMapping("/admin/logout")
    public String adminLogout(HttpSession session) {
        session.removeAttribute("adminLoggedIn");
        return "redirect:/admin/login";
    }

    // ── Generate ──────────────────────────────────────────────────────────────

    @PostMapping("/admin/generate")
    public String generateQuestion(HttpSession session, Model model, HttpServletResponse response) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        int nextLevel = questionRepo.findAll()
            .stream().mapToInt(q -> q.getLevelNumber()).max().orElse(0) + 1;
        model.addAttribute("nextLevel", nextLevel);
        model.addAttribute("activePlayers", sessionTracker.getActiveCount());

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

    // ── Upload image ──────────────────────────────────────────────────────────
    // SECURITY: Original filename is discarded entirely to prevent path traversal.
    // Extension is detected from content type; defaults to .jpg if unknown.

    @PostMapping("/admin/upload-image")
    @ResponseBody
    public Map<String, Object> uploadImage(@RequestParam MultipartFile file,
                                            HttpSession session,
                                            HttpServletResponse response) {
        if (!isAdmin(session)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Map.of("error", "Not authenticated");
        }
        try {
            // Derive a safe extension from content type only — original filename ignored
            String contentType = file.getContentType() != null ? file.getContentType() : "";
            String ext = contentType.contains("png") ? ".png"
                       : contentType.contains("gif") ? ".gif"
                       : contentType.contains("webp") ? ".webp"
                       : ".jpg";

            String safeFilename = UUID.randomUUID().toString() + ext;

            Path uploadDir = Paths.get("src/main/resources/static/uploads");
            Files.createDirectories(uploadDir);
            Files.write(uploadDir.resolve(safeFilename), file.getBytes());
            return Map.of("url", "/uploads/" + safeFilename);
        } catch (Exception e) {
            return Map.of("error", "Upload failed: " + e.getMessage());
        }
    }

    // ── Refetch image ─────────────────────────────────────────────────────────

    @GetMapping("/admin/refetch-image")
    @ResponseBody
    public Map<String, Object> refetchImage(@RequestParam String keyword,
                                             HttpSession session,
                                             HttpServletResponse response) {
        if (!isAdmin(session)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Map.of("error", "Not authenticated");
        }
        try {
            String url = unsplashService.getImageUrl(keyword);
            if (url == null || url.isBlank()) return Map.of("error", "No image found for: " + keyword);
            return Map.of("url", url);
        } catch (Exception e) {
            return Map.of("error", "Unsplash error: " + e.getMessage());
        }
    }

    // ── Approve ───────────────────────────────────────────────────────────────

   // ── Approve — now also tracks the answer as "used"
    @PostMapping("/admin/approve")
    public String approveQuestion(
        @RequestParam String answer,
        @RequestParam String imageUrl1, @RequestParam String imageUrl2, @RequestParam String imageUrl3,
        @RequestParam String clue1, @RequestParam String clue2, @RequestParam String clue3,
        @RequestParam int levelNumber,
        @RequestParam(required = false, defaultValue = "") String theme,  // NEW
        Model model) {

        questionRepo.save(new Question(clue1, clue2, clue3, imageUrl1, imageUrl2, imageUrl3, answer, levelNumber));

        // Track as used so Groq never suggests it again
        if (!rejectedAnswerRepo.existsByAnswerIgnoreCase(answer)) {
            rejectedAnswerRepo.save(new RejectedAnswer(answer, theme, true));
        }

        model.addAttribute("message", "✓ Question saved for Level " + levelNumber);
        return "admin";
    }

// ── Reject — now captures reason and theme@PostMapping("/admin/reject")
    public String rejectQuestion(HttpSession session, Model model, HttpServletResponse response,
                                @RequestParam(required = false) String answer,
                                @RequestParam(required = false, defaultValue = "") String reason,
                                @RequestParam(required = false, defaultValue = "") String theme) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        if (answer != null && !answer.isBlank())
            rejectedAnswerRepo.save(new RejectedAnswer(answer, reason, theme));
        return generateQuestion(session, model, response);  // ← all 3 args
    }

    @PostMapping("/admin/regenerate")
    public String regenerateQuestion(HttpSession session, Model model, HttpServletResponse response,
                                    @RequestParam(required = false) String answer,
                                    @RequestParam(required = false, defaultValue = "") String reason,
                                    @RequestParam(required = false, defaultValue = "") String theme) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        if (answer != null && !answer.isBlank())
            rejectedAnswerRepo.save(new RejectedAnswer(answer, reason, theme));
        return generateQuestion(session, model, response);  // ← all 3 args
    }
}