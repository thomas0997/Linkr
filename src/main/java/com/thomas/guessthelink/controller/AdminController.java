package com.thomas.guessthelink.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.thomas.guessthelink.*;
import com.thomas.guessthelink.services.*;
import com.thomas.guessthelink.repository.*;
import com.thomas.guessthelink.security.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    @Autowired GeminiService geminiService;
    @Autowired UnsplashService unsplashService;
    @Autowired QuestionRepository questionRepo;
    @Autowired TotpService totpService;
    @Autowired RateLimitService rateLimitService;

    // Put this in application.properties: admin.totp.secret=YOUR_SECRET
    // Leave blank until you run /admin/setup the first time.
    @Value("${admin.totp.secret:}")
    private String totpSecret;

    // ── SETUP (one-time) ──────────────────────────────────────────────────────

    @GetMapping("/admin/setup")
    public String showSetup(Model model) {
        if (totpSecret != null && !totpSecret.isBlank()) {
            return "redirect:/admin/login"; // already set up
        }
        String secret = totpService.generateSecret();
        String qrUrl  = totpService.getQRCodeImageUrl(secret, "admin", "Linkr");
        model.addAttribute("secret", secret);
        model.addAttribute("qrUrl", qrUrl);
        return "admin-setup";
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    @GetMapping("/admin/login")
    public String showLogin(
        @RequestParam(required = false) String error,
        @RequestParam(required = false) String logout,
        HttpServletRequest request,
        Model model
    ) {
        String ip = request.getRemoteAddr();
        if (rateLimitService.isBlocked(ip)) {
            model.addAttribute("error", "Too many failed attempts. Try again in "
                + rateLimitService.minutesRemaining(ip) + " minutes.");
            model.addAttribute("blocked", true);
            return "admin-login";
        }
        if (error != null)  model.addAttribute("error", "Invalid code. Try again.");
        if (logout != null) model.addAttribute("message", "Logged out.");
        return "admin-login";
    }

    @PostMapping("/admin/login-process")
    public String processLogin(
        @RequestParam String code,
        HttpServletRequest request,
        RedirectAttributes redirectAttrs
    ) {
        String ip = request.getRemoteAddr();

        if (rateLimitService.isBlocked(ip)) {
            redirectAttrs.addFlashAttribute("error", "Locked out. Try again later.");
            return "redirect:/admin/login";
        }

        if (totpSecret == null || totpSecret.isBlank()) {
            redirectAttrs.addFlashAttribute("error", "No secret configured. Visit /admin/setup first.");
            return "redirect:/admin/login";
        }

        if (!totpService.verify(totpSecret, code.trim())) {
            rateLimitService.recordFailure(ip);
            return "redirect:/admin/login?error";
        }

        // Correct code — log in via Spring Security
        rateLimitService.recordSuccess(ip);
        var auth = new UsernamePasswordAuthenticationToken(
            "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        request.getSession().setAttribute(
            "SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext()
        );

        return "redirect:/admin";
    }

    // ── ADMIN PANEL ───────────────────────────────────────────────────────────

    @GetMapping("/admin")
    public String showAdmin(Model model) {
        if (!model.containsAttribute("message")) {
            model.addAttribute("message", "Welcome to the admin panel.");
        }
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
            e.printStackTrace();
            model.addAttribute("error", "Generation failed: " + e.getMessage());
        }
        return "admin";
    }

    @PostMapping("/admin/regenerate")
    public String regenerateQuestion(Model model) {
        return generateQuestion(model);
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
        RedirectAttributes redirectAttrs
    ) {
        Question q = new Question(clue1, clue2, clue3, imageUrl1, imageUrl2, imageUrl3, answer, levelNumber);
        questionRepo.save(q);
        redirectAttrs.addFlashAttribute("message",
            "✓ Saved to DB — Level " + levelNumber + " | Answer: \u201c" + answer + "\u201d");
        return "redirect:/admin";
    }

    @PostMapping("/admin/reject")
    public String rejectQuestion(RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("message", "Question rejected. Generate a new one.");
        return "redirect:/admin";
    }

    @GetMapping("/admin/refetch-image")
    @ResponseBody
    public Map<String, String> refetchImage(@RequestParam String keyword) {
        try {
            String url = unsplashService.getImageUrl(keyword);
            if (url == null || url.isEmpty()) return Map.of("error", "No image found for: " + keyword);
            return Map.of("url", url);
        } catch (Exception e) {
            return Map.of("error", "Unsplash error: " + e.getMessage());
        }
    }
}