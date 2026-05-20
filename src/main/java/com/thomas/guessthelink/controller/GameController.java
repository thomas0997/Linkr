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
import java.util.*;
import jakarta.servlet.http.Cookie;

@Controller
public class GameController {

    @Autowired GameService gameService;
    @Autowired PlayerService playerService;
    @Autowired QuestionService questionService;
    @Autowired GameProgressRepository gameProgressRepo;
    @Autowired SessionTracker sessionTracker;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // ── Login / Register ──────────────────────────────────────────────────────

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

        Player player = playerService.findByUsername(username);

        if (player == null) {
            String hashed = passwordEncoder.encode(password);
            player = new Player(username, hashed, 0L, 1);
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

    // ── Guest ─────────────────────────────────────────────────────────────────

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
                    } catch (NumberFormatException e) { /* bad cookie */ }
                }
            }
        }

        long number = playerService.countGuests() + 1;
        String guestName = String.format("Guest[%02d]", number);
        Player guest = new Player(guestName, "", 0L, 1);
        playerService.savePlayer(guest);

        Cookie guestCookie = new Cookie("guestId", String.valueOf(guest.getId()));
        guestCookie.setMaxAge(30 * 24 * 60 * 60);
        guestCookie.setPath("/");
        response.addCookie(guestCookie);

        session.setAttribute("playerId", guest.getId());
        return "redirect:/home";
    }
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
    // ── Home ──────────────────────────────────────────────────────────────────

    @GetMapping("/home")
    public String showHome(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";

        sessionTracker.markActive(playerId);

        Player player = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());
        Question nextQuestion = questionService.getQuestionByLevel(player.getCurrentLevel() + 1);
        boolean hasNextLevel = nextQuestion != null || question != null;

        model.addAttribute("player", player);
        model.addAttribute("question", question);
        model.addAttribute("hasNextLevel", hasNextLevel);
        return "home";
    }

    // ── Game ──────────────────────────────────────────────────────────────────

    @GetMapping("/game")
    public String showGame(Model model, HttpSession session) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";

        sessionTracker.markActive(playerId);

        Player player = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());

        boolean alreadyCompleted = false;
        if (question != null) {
            GameProgress progress = gameProgressRepo.findByPlayerIdAndQuestionId(playerId, question.getId());
            alreadyCompleted = progress != null && progress.getIsComplete();
        }

        // Build word-length pattern (e.g. "******* ******") — no letters revealed
        // This lets the JS show blank slots per word without exposing the answer
        String answerPattern = "";
        if (question != null && question.getAnswer() != null) {
            answerPattern = question.getAnswer().replaceAll("[^ ]", "*");
        }

        model.addAttribute("player", player);
        model.addAttribute("question", question);
        model.addAttribute("alreadyCompleted", alreadyCompleted);
        model.addAttribute("answerPattern", answerPattern);
        return "game";
    }

    // ── Guess — server checks the answer, calculates coins ────────────────────
    // Answer never goes to the browser. Tries tracked in session so client
    // cannot inflate the coin reward by sending a fake tries count.

    @PostMapping("/guess")
    @ResponseBody
    public Map<String, Object> handleGuess(@RequestParam String guess, HttpSession session) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return Map.of("error", "not logged in");

        Player player = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());
        if (question == null) return Map.of("correct", false);

        // Track tries server-side so coins cannot be manipulated by the client
        String triesKey = "tries_" + question.getId();
        Integer triesObj = (Integer) session.getAttribute(triesKey);
        int tries = (triesObj == null ? 0 : triesObj) + 1;
        session.setAttribute(triesKey, tries);

        boolean correct = question.getAnswer().equalsIgnoreCase(guess.trim());

        if (correct) {
            int coinsEarned = calculateCoins(tries);
            playerService.addCoins(playerId, (long) coinsEarned);

            // Save progress (only if not already saved)
            GameProgress existing = gameProgressRepo.findByPlayerIdAndQuestionId(playerId, question.getId());
            if (existing == null) {
                gameProgressRepo.save(new GameProgress(playerId, question.getId(), tries, true));
            }

            // Unlock next level and check if it exists
            int nextLevel = player.getCurrentLevel() + 1;
            boolean hasNext = questionService.getQuestionByLevel(nextLevel) != null;
            if (hasNext) gameService.unlockNextLevel(playerId);

            // Clear tries for this question from session
            session.removeAttribute(triesKey);

            return Map.of("correct", true, "coinsEarned", coinsEarned, "hasNext", hasNext);
        }

        return Map.of("correct", false, "tries", tries);
    }

    private int calculateCoins(int tries) {
        if (tries == 1)       return 5;
        if (tries <= 3)       return 4;
        if (tries <= 5)       return 3;
        if (tries <= 8)       return 2;
        if (tries <= 10)      return 1;
        return 0;
    }

    // ── Clue — returns clue TEXT from server, not from hidden HTML input ──────

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

        // Fetch clue text here on the server — never stored in HTML
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());
        String clueText = clueNumber == 1 ? question.getClueOne()
                        : clueNumber == 2 ? question.getClueTwo()
                        : question.getClueThree();

        return Map.of("success", true, "coins", player.getCoins(), "clueText", clueText);
    }

    // ── Letter clue — server picks and returns one unrevealed letter ──────────

    @PostMapping("/use-letter-clue")
    @ResponseBody
    public Map<String, Object> handleLetterClue(
            @RequestParam(required = false, defaultValue = "") String revealed,
            HttpSession session) {

        Long playerId = (Long) session.getAttribute("playerId");
        Player player = playerService.getPlayerId(playerId);
        int cost = 10;

        if (player.getCoins() < cost) {
            return Map.of("success", false, "coins", player.getCoins());
        }

        Player refreshed = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(refreshed.getCurrentLevel());
        String answer = question.getAnswer();

        // Parse which positions the client already revealed
        Set<Integer> revealedSet = new HashSet<>();
        if (!revealed.isBlank()) {
            for (String s : revealed.split(",")) {
                try { revealedSet.add(Integer.parseInt(s.trim())); }
                catch (NumberFormatException ignored) {}
            }
        }

        // Collect unrevealed non-space positions
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < answer.length(); i++) {
            if (answer.charAt(i) != ' ' && !revealedSet.contains(i)) {
                candidates.add(i);
            }
        }

        if (candidates.isEmpty()) {
            return Map.of("success", false, "coins", player.getCoins(), "message", "All letters revealed");
        }

        playerService.addCoins(playerId, (long) -cost);
        refreshed = playerService.getPlayerId(playerId);

        int idx = candidates.get(new Random().nextInt(candidates.size()));
        String letter = String.valueOf(answer.charAt(idx)).toUpperCase();

        return Map.of("success", true, "coins", refreshed.getCoins(), "index", idx, "letter", letter);
    }

    // ── Leaderboard ───────────────────────────────────────────────────────────

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

    // ── About / Tutorial / Profile ────────────────────────────────────────────

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

     
    /*@GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";
        model.addAttribute("player", playerService.getPlayerId(playerId));
        return "profile";
    }*/

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";
        
        Player player = playerService.getPlayerId(playerId);
        List<Player> leaderboard = playerService.getLeaderboard();
        
        model.addAttribute("player", player);
        model.addAttribute("leaderboard", leaderboard);
        model.addAttribute("levelsCompleted", player.getCurrentLevel() - 1); 
        
        return "profile";
    }
}