package com.thomas.guessthelink.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.thomas.guessthelink.repository.PlayerRepository;
import com.thomas.guessthelink.Player;

@Service
public class GameService {

    @Autowired PlayerRepository playerRepo;

    // submitGuess() and useClue() removed — GameController handles both directly
    // so that tries and coins are tracked in the HTTP session (tamper-proof).

    public Player unlockNextLevel(Long playerId) {
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player == null) return null;
        player.setCurrentLevel(player.getCurrentLevel() + 1);
        return playerRepo.save(player);
    }
}