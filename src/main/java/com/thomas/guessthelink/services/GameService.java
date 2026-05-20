package com.thomas.guessthelink.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.thomas.guessthelink.repository.GameProgressRepository;
import com.thomas.guessthelink.repository.PlayerRepository;
import com.thomas.guessthelink.repository.QuestionRepository;
import com.thomas.guessthelink.*;

@Service
public class GameService {

    @Autowired QuestionRepository questionRepo;
    @Autowired PlayerRepository playerRepo;
    @Autowired GameProgressRepository gameProgressRepo;
    @Autowired PlayerService playerService;

    public GuessResult submitGuess(Long playerId, int levelNumber, String guess,
                                   int triesUsed, int coinsUsed) {
        Question question = questionRepo.findByLevelNumber(levelNumber);
        boolean isCorrect = question.getAnswer().equalsIgnoreCase(guess);

        if (isCorrect) {
            int coins = calculateCoins(triesUsed);
            playerService.addCoins(playerId, (long) coins);
            gameProgressRepo.save(new GameProgress(playerId, question.getId(), triesUsed, true));
            return new GuessResult(true, coins);
        }

        return new GuessResult(false, 0);
    }

    private int calculateCoins(int triesUsed) {
        if (triesUsed == 1)      return 5;
        else if (triesUsed <= 3) return 4;
        else if (triesUsed <= 5) return 3;
        else if (triesUsed <= 8) return 2;
        else if (triesUsed <= 10)return 1;
        else                     return 0;
    }

    public int useClue(Long playerId, int clueNumber, int levelNumber) {
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player == null) return 0;

        int clueCost = getClueCost(clueNumber);
        if (player.getCoins() < clueCost) return 0;

        player.setCoins(player.getCoins() - clueCost);
        playerRepo.save(player);
        return clueCost;
    }

    private int getClueCost(int clueNumber) {
        if (clueNumber == 1) return 2;
        if (clueNumber == 2) return 4;
        if (clueNumber == 3) return 8;
        return 0;
    }

    public Player unlockNextLevel(Long playerId) {
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player == null) return null;
        player.setCurrentLevel(player.getCurrentLevel() + 1);
        return playerRepo.save(player);
    }
}