package com.thomas.guessthelink.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.thomas.guessthelink.services.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import com.thomas.guessthelink.*;





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

        session.setAttribute("playerId", player.getId());
        return "redirect:/home";

    }
    
    @GetMapping("/game")
    public String showGame(Model model, HttpSession session) {
        Long playerId = (Long) session.getAttribute("playerId");
        if (playerId == null) return "redirect:/";  // send back to login
        
        Player player = playerService.getPlayerId(playerId);
        Question question = questionService.getQuestionByLevel(player.getCurrentLevel());
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
    public String nextLevel(HttpSession session){
        Long playerId =  (Long) session.getAttribute("playerId");
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
    
}

