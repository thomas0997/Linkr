package com.thomas.guessthelink;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity // Tells Spring this is a db class
@Table(name = "player") // Tells the name of the db

public class Player{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto increment the id for me.

    private Long id;
    private Long coins;
    private int currentLevel;
    private String username;

    public Player(){}
    public Player(String username, Long coins, int currentLevel){
        this.username = username;
        this.coins = coins;
        this.currentLevel = currentLevel;
    }

    public Long getId(){ return id; }
    public Long getCoins(){ return coins; }
    public int getCurrentLevel(){ return currentLevel; }
    public String getUsername(){ return username; }
    public void setCoins(Long coins){ this.coins = coins; }
    public void setCurrentLevel(int currentLevel){ this.currentLevel = currentLevel; }


}