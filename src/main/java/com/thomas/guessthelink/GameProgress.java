package com.thomas.guessthelink;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "game_progress")
public class GameProgress{
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;
    private Long playerId, questionId;
    private int triesUsed;
    private Boolean isComplete;

    public GameProgress(){ }

    public GameProgress(Long playerId, Long questionId, int triesUsed, boolean isComplete){
        this.playerId = playerId;
        this.questionId = questionId;
        this.triesUsed = triesUsed;
        this.isComplete = isComplete;
    }

    public Long getId(){return id;}
    public Long getPlayerId(){return playerId;}
    public int getTries(){ return triesUsed; }
    public boolean getIsComplete(){ return isComplete; }
}