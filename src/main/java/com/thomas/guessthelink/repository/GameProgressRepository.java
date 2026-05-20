package com.thomas.guessthelink.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thomas.guessthelink.GameProgress;


public interface GameProgressRepository extends JpaRepository<GameProgress, Long>
{   
    // this is used in the class GameService, in the method submitGuess, to find the game progress of a specific player and question.
    GameProgress findByPlayerIdAndQuestionId(Long playerId, Long questionId);

}
