package com.thomas.guessthelink.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.thomas.guessthelink.*;
import com.thomas.guessthelink.services.*;
import com.thomas.guessthelink.repository.*;
import com.thomas.guessthelink.security.TotpService;
import jakarta.servlet.http.HttpSession;

@Controller
public class AdminController {

    @Autowired GeminiService geminiService;
    @Autowired UnsplashService unsplashService;
    @Autowired QuestionRepository questionRepo;
    @Autowired TotpService totpService;

    @Value("${admin.totp.secret:}")
    private String totpSecret;

    // ── /admin/setup ──────────────────────────────────────────────────────────

    @GetMapping("/admin/setup")
    public String showSetup(Model model) {
        if (totpSecret != null && !totpSecret.isEmpty()) {
            return "redirect:/admin/login";
        }
        String secret = totpService.generateSecret();
        String qrUrl  = totpService.getQRCodeImageUrl(secret, "admin", "Linkr");
        model.addAttribute("secret", secret);
        model.addAttribute("qrUrl", qrUrl);
        return "admin-setup";
    }

    // ── /admin/login ──────────────────────────────────────────────────────────

    @GetMapping("/admin/login")
    public String showAdminLogin(HttpSession session) {
        if (Boolean.TRUE.equals(session.getAttribute("adminAuth"))) {
            return "redirect:/admin";
        }
        return "admin-login";
    }

    @PostMapping("/admin/login")
    public String handleAdminLogin(
            @RequestParam String code,
            HttpSession session,
            Model model) {

        // ── DEBUG — remove these prints once login is working ──────────────
        System.out.println("=== ADMIN LOGIN ATTEMPT ===");
        System.out.println("Secret in properties : [" + totpSecret + "]");
        System.out.println("Code entered by user : [" + code + "]");
        System.out.println("Secret is empty?     : " + (totpSecret == null || totpSecret.isEmpty()));

        boolean result = totpService.verify(totpSecret, code);
        System.out.println("verify() returned    : " + result);
        System.out.println("===========================");
        // ──────────────────────────────────────────────────────────────────

        if (totpSecret == null || totpSecret.isEmpty()) {
            return "redirect:/admin/setup";
        }

        if (result) {
            session.setAttribute("adminAuth", true);
            return "redirect:/admin";
        }

        model.addAttribute("error", "Invalid code — try again.");
        return "admin-login";
    }

    // ── /admin (panel) ────────────────────────────────────────────────────────

    @GetMapping("/admin")
    public String showAdmin(Model model, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("adminAuth"))) {
            return "redirect:/admin/login";
        }
        model.addAttribute("message", "Welcome to the admin panel.");
        return "admin";
    }

    // ── Question generation ───────────────────────────────────────────────────

    @PostMapping("/admin/generate")
    public String generateQuestion(Model model, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("adminAuth"))) {
            return "redirect:/admin/login";
        }
        try {
            GeneratedQuestion q = geminiService.generateQuestion();

            System.out.println("=== GEMINI OUTPUT ===");
            System.out.println("Answer: "   + q.getAnswer());
            System.out.println("Keyword1: " + q.getImageKeyword1());
            System.out.println("Keyword2: " + q.getImageKeyword2());
            System.out.println("Keyword3: " + q.getImageKeyword3());
            System.out.println("Clue1: "    + q.getClue1());

            String img1 = unsplashService.getImageUrl(q.getImageKeyword1());
            String img2 = unsplashService.getImageUrl(q.getImageKeyword2());
            String img3 = unsplashService.getImageUrl(q.getImageKeyword3());

            System.out.println("=== UNSPLASH OUTPUT ===");
            System.out.println("Image1: " + img1);
            System.out.println("Image2: " + img2);
            System.out.println("Image3: " + img3);

            q.setImageUrl1(img1);
            q.setImageUrl2(img2);
            q.setImageUrl3(img3);

            model.addAttribute("generated", q);
            model.addAttribute("message", "Review the question below.");

        } catch (Exception e) {
            System.out.println("=== ERROR ===");
            e.printStackTrace();
            model.addAttribute("error", "Generation failed: " + e.getMessage());
        }
        return "admin";
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
            Model model,
            HttpSession session) {

        if (!Boolean.TRUE.equals(session.getAttribute("adminAuth"))) {
            return "redirect:/admin/login";
        }
        Question q = new Question(clue1, clue2, clue3, imageUrl1, imageUrl2, imageUrl3, answer, levelNumber);
        questionRepo.save(q);
        model.addAttribute("message", "✓ Question saved to DB for Level " + levelNumber);
        return "admin";
    }

    @PostMapping("/admin/reject")
    public String rejectQuestion(Model model, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("adminAuth"))) return "redirect:/admin/login";
        model.addAttribute("message", "Question rejected. Generate a new one.");
        return "admin";
    }

    @PostMapping("/admin/regenerate")
    public String regenerateQuestion(Model model, HttpSession session) {
        return generateQuestion(model, session);
    }

    @PostMapping("/admin/logout")
    public String adminLogout(HttpSession session) {
        session.removeAttribute("adminAuth");
        return "redirect:/admin/login";
    }
}