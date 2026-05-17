package com.thomas.guessthelink;

import jakarta.persistence.*;

@Entity
@Table(name = "player")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long coins;
    private int currentLevel;
    private String username;
    private String password; // ← ADD

    public Player() {}
    public Player(String username, String password, Long coins, int currentLevel) {
        this.username = username;
        this.password = password; // ← ADD
        this.coins = coins;
        this.currentLevel = currentLevel;
    }

    public Long getId()           { return id; }
    public Long getCoins()        { return coins; }
    public int getCurrentLevel()  { return currentLevel; }
    public String getUsername()   { return username; }
    public String getPassword()   { return password; } // ← ADD
    public void setCoins(Long coins)              { this.coins = coins; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
}