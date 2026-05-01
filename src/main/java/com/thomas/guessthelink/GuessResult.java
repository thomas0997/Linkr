package com.thomas.guessthelink;

public class GuessResult {
    private boolean correct;
    private int coinsEarned;

    public GuessResult(boolean correct, int coinsEarned) {
        this.correct = correct;
        this.coinsEarned = coinsEarned;
    }

    public boolean isCorrect() { return correct; }
    public int getCoinsEarned() { return coinsEarned; }
}