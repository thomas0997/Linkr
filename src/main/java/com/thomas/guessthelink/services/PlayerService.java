package com.thomas.guessthelink.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.thomas.guessthelink.Player;
import com.thomas.guessthelink.repository.PlayerRepository;
import java.util.*;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepo;

    public Player getPlayerId(Long id) {
        return playerRepo.findById(id).orElse(null);
    }

    public Player savePlayer(Player player) {
        return playerRepo.save(player);
    }

    public Player addCoins(Long id, Long coins) {
        Player player = playerRepo.findById(id).orElse(null);
        if (player == null) return null;
        player.setCoins(player.getCoins() + coins);
        return playerRepo.save(player);
    }

    public Player updateLevel(Long id, int level) {
        Player player = playerRepo.findById(id).orElse(null);
        if (player == null) return null;
        player.setCurrentLevel(level);
        return playerRepo.save(player);
    }

    // Case-insensitive: "Thomas" and "thomas" resolve to the same account
    public Player findByUsername(String username) {
        return playerRepo.findByUsernameIgnoreCase(username);
    }

    public List<Player> getLeaderboard() {
        return playerRepo.findAllByOrderByCurrentLevelDesc();
    }

    public long countGuests() {
        return playerRepo.countByUsernameStartingWith("Guest[");
    }
}