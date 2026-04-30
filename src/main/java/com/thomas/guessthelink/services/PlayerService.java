package com.thomas.guessthelink.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.thomas.guessthelink.Player;
import com.thomas.guessthelink.repository.PlayerRepository;
import java.util.List;


//
@Service
public class PlayerService {

    // @Autowired tells Spring to find the already-registered 
    // PlayerRepository from its inventory and inject it here automatically, 
    // so we don't create it manually.
    @Autowired
    private PlayerRepository playerRepo;

    
    // Player as return type as it modifies a player and returns player OBJECt to whovever calls it
    // Gets Player Id from  built it from JPA
    public Player getPlayerId(Long id) {
        return playerRepo.findById(id).orElse(null);
    }


    // Saves player
    public Player savePlayer(Player players) {
        return playerRepo.save(players);
    }


    // Adds coin by delcaring a player object first that finds its id, if no id is found then null
    // for that player object, set the coins to what to add and current. then Save
    public Player addCoins(Long id, Long coins) {
        Player player = playerRepo.findById(id).orElse(null);
        if (player == null) return null;
        player.setCoins(player.getCoins() + coins);
        return playerRepo.save(player);
    }

    // Get the id from Player.java and level
    // Initialize to player variable by getting its id, if none, then null
    // Set the current object players to the current level

    public Player updateLevel(Long id, int level) {
        Player player = playerRepo.findById(id).orElse(null);
        player.setCurrentLevel(level);
        return playerRepo.save(player);
        }


}
