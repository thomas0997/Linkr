package com.thomas.guessthelink;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unsplash URLs can easily exceed 255 chars — use 1000 to be safe
    @Column(length = 1000)
    private String imageOne;

    @Column(length = 1000)
    private String imageTwo;

    @Column(length = 1000)
    private String imageThree;

    // Clues can also be long sentences
    @Column(length = 500)
    private String clueOne;

    @Column(length = 500)
    private String clueTwo;

    @Column(length = 500)
    private String clueThree;

    private String answer;
    private int levelNumber;

    public Question() {}

    public Question(String clueOne, String clueTwo, String clueThree,
                    String imageOne, String imageTwo, String imageThree,
                    String answer, int levelNumber) {
        this.clueOne = clueOne;
        this.clueTwo = clueTwo;
        this.clueThree = clueThree;
        this.imageOne = imageOne;
        this.imageTwo = imageTwo;
        this.imageThree = imageThree;
        this.answer = answer;
        this.levelNumber = levelNumber;
    }

    public String getClueOne()   { return clueOne; }
    public String getClueTwo()   { return clueTwo; }
    public String getClueThree() { return clueThree; }
    public String getImageOne()  { return imageOne; }
    public String getImageTwo()  { return imageTwo; }
    public String getImageThree(){ return imageThree; }
    public String getAnswer()    { return answer; }
    public int getLevelNumber()  { return levelNumber; }
    public Long getId()          { return id; }
}