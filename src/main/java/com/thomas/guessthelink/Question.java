package com.thomas.guessthelink;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity // Tells Spring this is a db class
@Table(name = "questions") // Tells the name of the db

public class Question {
    private String clueOne, clueTwo, clueThree;
    private String imageOne, imageTwo, imageThree, answer;
    private int levelNumber;

    @Id // Identifier of the primary key? iddk what that means
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto increment the id for me.
    private Long id;


    public Question(){} // A constructor that has no args fgor default.
    public Question(String clueOne, String clueTwo, String clueThree, String imageOne, String imageTwo, String imageThree, String answer, int levelNumber){
        this.clueOne = clueOne;
        this.clueTwo = clueTwo;
        this.clueThree = clueThree;
        this.imageOne = imageOne;
        this.imageTwo = imageTwo;
        this.imageThree = imageThree;
        this.answer = answer;
        this.levelNumber = levelNumber;
    }

    public String getClueOne(){ return clueOne; }
    public String getClueTwo(){ return clueTwo; }
    public String getClueThree(){ return clueThree; }
    public String getImageThree(){ return imageThree; }
    public String getAnswer(){ return answer; }
    public int getLevelNumber(){ return levelNumber; }
    public Long getID(){ return id; }
    
}
