package com.thomas.guessthelink.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thomas.guessthelink.repository.GameProgressRepository;
import com.thomas.guessthelink.repository.PlayerRepository;
import com.thomas.guessthelink.repository.QuestionRepository;
import com.thomas.guessthelink.*;

@Service
public class GameService {


    @Autowired
    QuestionRepository questionRepo;

    @Autowired
    PlayerRepository playerRepo;

    @Autowired
    GameProgressRepository gameProgressRepo;

    @Autowired
    PlayerService playerService;




    // Method to submit a guess for a specific level and player
    public GuessResult submitGuess(Long playerId, int levelNumber, String guess, int triesUsed, int coinsUsed){
        // Question variable instantiates from questionRepo and finds the question by level number, which is a 
        // method I declared in QuestionRepository

        // Level Number is an argument that is passed in from the controller when the user submits a guess, 
        // and it is used to find the corresponding question for that level.

        Question question = questionRepo.findByLevelNumber(levelNumber); 
        //quesionRepo is the instance of QuestionRepository that is automatically injected by Spring, 
        // and it provides the method findByLevelNumber to retrieve the question based on the level number.

        //Tl;dr it gets the question based on the current level number
        
        boolean isCorrect = question.getAnswer().equalsIgnoreCase(guess);
        // isCorrect is a bool that gets the answer from user input
        //question method came from the instantiated question variable
        // getAnswer() is a method in the Question class that retrieves the correct answer for that question.



        if (isCorrect)
        {
            int coins = calculateCoins(triesUsed);
            playerService.addCoins(playerId,(long) coins);

            GameProgress progress = new GameProgress(playerId, question.getID(), triesUsed, true);
            gameProgressRepo.save(progress);

            return new GuessResult(true, coins);
        } 
        
        else 
        {
            return new GuessResult(false, 0);
        }



    
    }
        // Calculate coins based on tries used and update player's coins and level
    private int calculateCoins(int triesUsed){
        if (triesUsed == 1) return 5;
        else if (triesUsed <= 3 ) return 4;
        else if ( triesUsed <= 5 ) return 3;
        else if (triesUsed <= 8 ) return 2;
        else if (triesUsed <= 10) return 1;
        else return 0;


    }

    public int useClue(Long playerId, int clueNumber, int levelNumeber){
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player  == null ){
            return 0; // Player not found
        }
        
        int clueCost = getClueCost(clueNumber);;
        if (player.getCoins() < clueCost){
            return 0; // Not enough coins
        } 

        player.setCoins(player.getCoins() - clueCost); 
        // Calculates the coins after using the clue and sets it to the player object
        
        playerRepo.save(player);
        //Saves the updated player object with the new coin count back to the database
        return clueCost;
    }


    private int getClueCost(int clueNumber)
    {
        if (clueNumber == 1) return 2;
        if (clueNumber == 2) return 4;
        if (clueNumber == 3) return 8;
        return 0;
    }

    public Player unlockNextLevel(Long playerId)
    {
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player == null) return null;
        player.setCurrentLevel(player.getCurrentLevel() + 1);
        return playerRepo.save(player);

    }

}
