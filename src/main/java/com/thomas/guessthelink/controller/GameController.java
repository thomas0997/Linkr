package com.thomas.guessthelink.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import com.thomas.guessthelink.*;
import com.thomas.guessthelink.services.*;
import com.thomas.guessthelink.repository.GameProgressRepository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Cookie;
import java.util.*;

@Controller
public class GameController {

    @Autowired GameService gameService;
    @Autowired PlayerService playerService;
    @Autowired QuestionService questionService;
    @Autowired GameProgressRepository gameProgressRepo;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // ── Login page ──────────────────────────────────────────────────────────
    @GetMapping("/")
    public String showLogin(Model model) {
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session) {

        if (!username.matches("[a-zA-Z0-9_]{3,20}")) {
            return "redirect:/?error=invalid_username";
        }

        Player player = playerService.findByUsername(username); // case-insensitive

        if (player == null) {
            String hashed = passwordEncoder.encode(password);
            player = new Player(username, hashed, 10L, 1); // start with 10 coins
            playerService.savePlayer(player);
            session.setAttribute("playerId", player.getId());
            return "redirect:/home";
        }

        if (!passwordEncoder.matches(password, player.getPassword())) {
            return "redirect:/?error=wrong_password";
        }

        session.setAttribute("playerId", player.getId());
        return "redirect:/home";
    }

    // ── Logout ──────────────────────────────────────────────────────────────
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ── Guest ───────────────────────────────────────────────────────────────
    @PostMapping("/guest")
    public String handleGuest(HttpSession session,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("guestId".equals(cookie.getName())) {
                    try {
                        Long existingId = Long.parseLong(cookie.getValue());
                        Player existing = playerService.getPlayerId(existingId);
                        if (existing != null && existing.getUsername().startsWith("Guest[")) {
                            session.setAttribute("playerId", existingId);
                            return "redirect:/home";
                        }
                    } catch (NumberFormatException e) { /* bad cookie, ignore */ }
                }
            }
        }

        long number = playerService.countGuests() + 1;
        String guestName = String.format("Guest[%02d]", number);
        Player guest = new Player(guestName, "", 10L, 1); // start with 10 coins
        playerService.savePlayer(guest);

        Cookie guestCookie = new Cookie("guestId", String.valueOf(guest.getId()));
        guestCookie.setMaxAge(30 * 24 * 60 * 60);
        guestCookie.setPath("/");
        response.addCookie(guestCookie);

        session.setAttribute("playerId", guest.getId());
        return "redirect:/home";
    }

    // ── Game ────────────────────────────────────────────────────────────────
    @GetMapping("/game")
    public String showGame(Model model, HttpSession session) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";

        Player player = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());

        boolean alreadyCompleted = false;
        if (question != null) {
            GameProgress progress = gameProgressRepo.findByPlayerIdAndQuestionId(playerId, question.getId());
            alreadyCompleted = progress != null && progress.getIsComplete();
        }

        model.addAttribute("player", player);
        model.addAttribute("question", question);
        model.addAttribute("alreadyCompleted", alreadyCompleted);
        return "game";
    }

    // ── Clue ────────────────────────────────────────────────────────────────
    @PostMapping("/use-clue")
    @ResponseBody
    public Map<String, Object> handleUseClue(@RequestParam int clueNumber, HttpSession session) {
        Long playerId = (Long) session.getAttribute("playerId");
        Player player = playerService.getPlayerId(playerId);
        int cost = clueNumber == 1 ? 2 : clueNumber == 2 ? 4 : 8;

        if (player.getCoins() < cost) {
            return Map.of("success", false, "coins", player.getCoins());
        }

        playerService.addCoins(playerId, (long) -cost);
        player = playerService.getPlayerId(playerId);
        return Map.of("success", true, "coins", player.getCoins());
    }

    @PostMapping("/use-letter-clue")
    @ResponseBody
    public Map<String, Object> handleLetterClue(HttpSession session) {
        Long playerId = (Long) session.getAttribute("playerId");
        Player player = playerService.getPlayerId(playerId);
        int cost = 10;

        if (player.getCoins() < cost) {
            return Map.of("success", false, "coins", player.getCoins());
        }

        playerService.addCoins(playerId, (long) -cost);
        player = playerService.getPlayerId(playerId);
        return Map.of("success", true, "coins", player.getCoins());
    }

    // ── Next level ──────────────────────────────────────────────────────────
    // Called immediately on correct guess (before animation finishes) so the
    // level increment is persisted even if the player navigates away early.
    @PostMapping("/next")
    @ResponseBody
    public Map<String, Object> nextLevel(HttpSession session,
                                         @RequestParam(defaultValue = "0") int coinsEarned) {
        Long playerId = (Long) session.getAttribute("playerId");
        Player player = playerService.getPlayerId(playerId);
        int nextLevel = player.getCurrentLevel() + 1;
        Question nextQuestion = questionService.getQuestionByLevel(nextLevel);

        if (coinsEarned > 0) playerService.addCoins(playerId, (long) coinsEarned);

        if (nextQuestion == null) {
            return Map.of("hasNext", false);
        }

        gameService.unlockNextLevel(playerId);
        return Map.of("hasNext", true);
    }

    // ── Save progress (called on correct guess) ─────────────────────────────
    @PostMapping("/guess-complete")
    @ResponseBody
    public Map<String, Object> guessComplete(@RequestParam int levelNumber, HttpSession session) {
        Long playerId = (Long) session.getAttribute("playerId");
        Question question = questionService.getQuestionByLevel(levelNumber);

        if (question != null) {
            GameProgress progress = new GameProgress(playerId, question.getId(), 0, true);
            gameProgressRepo.save(progress);
        }
        return Map.of("saved", true);
    }

    // ── Home ────────────────────────────────────────────────────────────────
    @GetMapping("/home")
    public String showHome(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";

        Player player = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());
        Question nextQuestion = questionService.getQuestionByLevel(player.getCurrentLevel() + 1);
        boolean hasNextLevel = nextQuestion != null || question != null;

        model.addAttribute("player", player);
        model.addAttribute("question", question);
        model.addAttribute("hasNextLevel", hasNextLevel);
        return "home";
    }

    // ── Leaderboard ─────────────────────────────────────────────────────────
    @GetMapping("/leaderboard")
    public String showLeaderboard(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";

        Player player = playerService.getPlayerId(playerId);
        List<Player> players = playerService.getLeaderboard();
        int playerRank = players.indexOf(player) + 1;

        model.addAttribute("player", player);
        model.addAttribute("players", players);
        model.addAttribute("playerRank", playerRank);
        model.addAttribute("totalPlayers", players.size());
        return "leaderboard";
    }

    // ── Static pages ────────────────────────────────────────────────────────
    @GetMapping("/about")
    public String showAbout(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";
        model.addAttribute("player", playerService.getPlayerId(playerId));
        return "about";
    }

    @GetMapping("/tutorial")
    public String showTutorial(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";
        model.addAttribute("player", playerService.getPlayerId(playerId));
        return "tutorial";
    }

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";

        Player player = playerService.getPlayerId(playerId);
        List<GameProgress> allProgress = gameProgressRepo.findAll()
            .stream()
            .filter(p -> p.getPlayerId().equals(playerId))
            .toList();

        long levelsCompleted = allProgress.stream().filter(GameProgress::getIsComplete).count();

        model.addAttribute("player", player);
        model.addAttribute("levelsCompleted", levelsCompleted);
        model.addAttribute("leaderboard", playerService.getLeaderboard());
        return "profile";
    }
}