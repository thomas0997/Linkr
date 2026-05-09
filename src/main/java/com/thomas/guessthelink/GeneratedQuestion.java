package com.thomas.guessthelink;

public class GeneratedQuestion {
    private String answer;
    private String imageUrl1, imageUrl2, imageUrl3;
    private String imageKeyword1, imageKeyword2, imageKeyword3;
    private String clue1, clue2, clue3;
    private int levelNumber;

    
    public GeneratedQuestion() {}

    public GeneratedQuestion(String answer, 
                              String imageUrl1, String imageUrl2, String imageUrl3,
                              String imageKeyword1, String imageKeyword2, String imageKeyword3,
                              String clue1, String clue2, String clue3) {
        this.answer = answer;
        this.imageUrl1 = imageUrl1;
        this.imageUrl2 = imageUrl2;
        this.imageUrl3 = imageUrl3;
        this.imageKeyword1 = imageKeyword1;
        this.imageKeyword2 = imageKeyword2;
        this.imageKeyword3 = imageKeyword3;
        this.clue1 = clue1;
        this.clue2 = clue2;
        this.clue3 = clue3;
    }

    // getters and setters for all fields
    public String getAnswer(){ return answer; }
    public String getImageUrl1(){ return imageUrl1; }
    public String getImageUrl2(){ return imageUrl2; }
    public String getImageUrl3(){ return imageUrl3; }
    public String getImageKeyword1(){ return imageKeyword1; }
    public String getImageKeyword2(){ return imageKeyword2; }
    public String getImageKeyword3(){ return imageKeyword3; }
    public String getClue1(){ return clue1; }
    public String getClue2(){ return clue2; }
    public String getClue3(){ return clue3; }
    public int getLevelNumber(){ return levelNumber; }
    public void setLevelNumber(int levelNumber){ this.levelNumber = levelNumber; }
    public void setImageUrl1(String imageUrl1){ this.imageUrl1 = imageUrl1; }
    public void setImageUrl2(String imageUrl2){ this.imageUrl2 = imageUrl2; }
    public void setImageUrl3(String imageUrl3){ this.imageUrl3 = imageUrl3; }
}