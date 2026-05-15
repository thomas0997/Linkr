package com.thomas.guessthelink;

import jakarta.persistence.*;

@Entity
@Table(name = "rejected_answers")
public class RejectedAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String answer;

    public RejectedAnswer() {}

    public RejectedAnswer(String answer) {
        this.answer = answer;
    }

    public Long getId() { return id; }
    public String getAnswer() { return answer; }
}