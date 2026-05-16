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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;





@Controller
public class GameController {
    @Autowired
    GameService gameService;

    @Autowired
    PlayerService playerService;

    @Autowired
    QuestionService questionService;

    @Autowired 
    GameProgressRepository gameProgressRepo;




    
    @GetMapping("/")
    public String showLogin(Model model){
        return "login";
    }


    @PostMapping("/login")    
    public String handleLogin(@RequestParam String username, Model model, HttpSession session){
        // validate first before anything else
        if (!username.matches("[a-zA-Z0-9_]{3,20}")) {
            return "redirect:/?error=invalid_username";
        }

        Player player = playerService.findByUsername(username);
        if (player == null){
            player = new Player(username, 0L, 1);
            playerService.savePlayer(player);
        }

        session.setAttribute("playerId", player.getId());
        return "redirect:/home";
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

   @GetMapping("/game")
    public String showGame(Model model, HttpSession session) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";

        Player player = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());

        // check if already completed
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

        @PostMapping("/guest")
        public String handleGuest(HttpSession session, 
                                HttpServletRequest request, 
                                HttpServletResponse response) {
            // Check if this device already has a guest cookie
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("guestId".equals(cookie.getName())) {
                        try {
                            Long existingId = Long.parseLong(cookie.getValue());
                            Player existing = playerService.getPlayerId(existingId);
                            if (existing != null && existing.getUsername().startsWith("Guest[")) {
                                // Restore their session — same guest, same progress
                                session.setAttribute("playerId", existingId);
                                return "redirect:/home";
                            }
                        } catch (NumberFormatException e) { /* bad cookie, ignore */ }
                    }
                }
            }

            // No valid cookie — create a fresh numbered guest
            long number = playerService.countGuests() + 1;
            String guestName = String.format("Guest[%02d]", number);
            Player guest = new Player(guestName, 0L, 1);
            playerService.savePlayer(guest);

            // Save their id in a cookie — lasts 30 days
            Cookie guestCookie = new Cookie("guestId", String.valueOf(guest.getId()));
            guestCookie.setMaxAge(30 * 24 * 60 * 60);
            guestCookie.setPath("/");
            response.addCookie(guestCookie);

            session.setAttribute("playerId", guest.getId());
            return "redirect:/home";
        }
    @PostMapping("/clue")
    public String handleClue(@RequestParam int clueNumber, HttpSession session, Model model){
        
        Long playerId = (Long) session.getAttribute("playerId");  
        Player player = playerService.getPlayerId(playerId);
        int status = gameService.useClue((Long)playerId, (int) clueNumber, player.getCurrentLevel());

        
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());

        model.addAttribute("player", player);
        model.addAttribute("clueStatus", status);
        model.addAttribute("question", question);

        return "game";
    }

    @PostMapping("/next")
    @ResponseBody
    public Map<String, Object> nextLevel(HttpSession session, @RequestParam(defaultValue="0") int coinsEarned) {
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

    @GetMapping("/home")
    public String showHome(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";

        Player player = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());
        
        // check if next level exists
        Question nextQuestion = questionService.getQuestionByLevel(player.getCurrentLevel() + 1);
        boolean hasNextLevel = nextQuestion != null || question != null;

        model.addAttribute("player", player);
        model.addAttribute("question", question);
        model.addAttribute("hasNextLevel", hasNextLevel);
        return "home";
    }



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
    
    @PostMapping("/use-clue")
    @ResponseBody
    public Map<String, Object> handleUseClue(@RequestParam int clueNumber, HttpSession session) {
        Long playerId = (Long) session.getAttribute("playerId");
        Player player = playerService.getPlayerId(playerId);
        int cost = clueNumber == 1 ? 2 : clueNumber == 2 ? 4 : 8;
        
        if (player.getCoins() < cost) {
            return Map.of("success", false, "coins", player.getCoins());
        }
        
        playerService.addCoins(playerId, (long) -cost); // deduct
        player = playerService.getPlayerId(playerId);
        return Map.of("success", true, "coins", player.getCoins());
    }

    @PostMapping("/guess-complete")
    @ResponseBody
    public Map<String, Object> guessComplete(@RequestParam int levelNumber, HttpSession session) {
        Long playerId = (Long) session.getAttribute("playerId");
        Player player = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(levelNumber);
        
        if (question != null) {
            GameProgress progress = new GameProgress(playerId, question.getId(), 0, true);
            gameProgressRepo.save(progress);
            System.out.println("Progress saved for player: " + playerId + " question: " + question.getId());
        }
        return Map.of("saved", true);
    }

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
        model.addAttribute("player", playerService.getPlayerId(playerId));
        return "profile";
    }
}

