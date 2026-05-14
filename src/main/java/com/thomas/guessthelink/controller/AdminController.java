package com.thomas.guessthelink.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.thomas.guessthelink.*;
import com.thomas.guessthelink.services.*;
import com.thomas.guessthelink.repository.*;

import java.util.Map;

@Controller
public class AdminController {

    @Autowired GeminiService geminiService;
    @Autowired UnsplashService unsplashService;
    @Autowired QuestionRepository questionRepo;

    // ── GET /admin — main panel ──────────────────────────────────────────────
    @GetMapping("/admin")
    public String showAdmin(Model model) {
        // Flash attributes from redirect (approve/reject) land here automatically
        if (!model.containsAttribute("message")) {
            model.addAttribute("message", "Welcome to the admin panel.");
        }
        return "admin";
    }

    // ── POST /admin/generate — call Groq + Unsplash ──────────────────────────
    @PostMapping("/admin/generate")
    public String generateQuestion(Model model) {
        try {
            GeneratedQuestion q = geminiService.generateQuestion();

            System.out.println("=== GROQ OUTPUT ===");
            System.out.println("Answer:   " + q.getAnswer());
            System.out.println("Keyword1: " + q.getImageKeyword1());
            System.out.println("Keyword2: " + q.getImageKeyword2());
            System.out.println("Keyword3: " + q.getImageKeyword3());

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
            model.addAttribute("message", "Review the question below. Re-fetch any bad image before approving.");

        } catch (Exception e) {
            System.out.println("=== ERROR ===");
            e.printStackTrace();
            model.addAttribute("error", "Generation failed: " + e.getMessage());
        }
        return "admin";
    }

    // ── POST /admin/regenerate — same as generate ────────────────────────────
    @PostMapping("/admin/regenerate")
    public String regenerateQuestion(Model model) {
        return generateQuestion(model);
    }

    // ── POST /admin/approve — save to DB, then redirect (PRG) ───────────────
    // FIX: was returning "admin" view directly after POST → caused whitelabel
    //      on certain flows. Now redirects to GET /admin cleanly.
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
        RedirectAttributes redirectAttrs   // <-- replaces Model; survives the redirect
    ) {
        Question q = new Question(clue1, clue2, clue3, imageUrl1, imageUrl2, imageUrl3, answer, levelNumber);
        questionRepo.save(q);

        // addFlashAttribute puts the value in the session for exactly one redirect,
        // then it lands in the model of the GET /admin that follows.
        redirectAttrs.addFlashAttribute("message", "✓ Saved to DB — Level " + levelNumber + " | Answer: " + answer);
        return "redirect:/admin";
    }

    // ── POST /admin/reject — discard, then redirect (PRG) ───────────────────
    @PostMapping("/admin/reject")
    public String rejectQuestion(RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("message", "Question rejected. Generate a new one.");
        return "redirect:/admin";
    }

    // ── GET /admin/refetch-image — re-search Unsplash for a single keyword ──
    // NEW: called by JS when admin wants to swap out one bad image.
    // Returns JSON: { "url": "https://..." }
    @GetMapping("/admin/refetch-image")
    @ResponseBody
    public Map<String, String> refetchImage(@RequestParam String keyword) {
        try {
            String url = unsplashService.getImageUrl(keyword);
            if (url == null || url.isEmpty()) {
                return Map.of("error", "No image found for: " + keyword);
            }
            return Map.of("url", url);
        } catch (Exception e) {
            return Map.of("error", "Unsplash error: " + e.getMessage());
        }
    }
}