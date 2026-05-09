package com.thomas.guessthelink.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.thomas.guessthelink.services.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import com.thomas.guessthelink.*;
import java.util.*;





@Controller
public class GameController {
    @Autowired
    GameService gameService;

    @Autowired
    PlayerService playerService;

    @Autowired
    QuestionService questionService;




    
    @GetMapping("/")
    public String showLogin(Model model){
        return "login";
    }


    @PostMapping("/login")    
    public String handleLogin(@RequestParam String username, Model model, HttpSession session){
        Player player = playerService.findByUsername(username);

        if (player == null){
            player = new Player(username, 0L, 1);
            playerService.savePlayer(player);

            
        }
        if (!username.matches("[a-zA-Z0-9_]{3,20}")) {
            return "redirect:/?error=invalid_username";
        }

        session.setAttribute("playerId", player.getId());
        return "redirect:/home";

    }
    @GetMapping("/game")
    public String showGame(Model model, HttpSession session) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";

        Player player = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());
        
        // prevent accessing levels beyond current
        model.addAttribute("player", player);
        model.addAttribute("question", question);
        return "game";
    }
    @PostMapping("/guess")
    public String handleGuess(@RequestParam String guess, 
    @RequestParam int tries, HttpSession session, Model model){

        Long playerId = (Long) session.getAttribute("playerId");  
        Player player = playerService.getPlayerId(playerId);
        GuessResult result = gameService.submitGuess(playerId, player.getCurrentLevel(), guess, tries, 0);

        model.addAttribute("result", result);
        model.addAttribute("player", playerService.getPlayerId(playerId));
        return "result";
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
    public String nextLevel(HttpSession session, @RequestParam(defaultValue="0") int coinsEarned, Model model) {
        Long playerId = (Long) session.getAttribute("playerId");
        Player player = playerService.getPlayerId(playerId);
        
        int nextLevel = player.getCurrentLevel() + 1;
        Question nextQuestion = questionService.getQuestionByLevel(nextLevel);
        
        if (nextQuestion == null) {
            // no next level — stay, show message
            if (coinsEarned > 0) playerService.addCoins(playerId, (long) coinsEarned);
            return "redirect:/game?noNextLevel=true";
        }
        
        if (coinsEarned > 0) playerService.addCoins(playerId, (long) coinsEarned);
        gameService.unlockNextLevel(playerId);
        return "redirect:/game";
    }

    @GetMapping("/home")
    public String showHome(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";  // must be first

        Player player = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());
        model.addAttribute("player", player);
        model.addAttribute("question", question);
        return "home";
    }
    @PostMapping("/guest")
    public String handleGuest(HttpSession session) {
        Player guest = new Player("Guest", 0L, 1);
        playerService.savePlayer(guest);
        session.setAttribute("playerId", guest.getId());
        return "redirect:/home";
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
    
}

