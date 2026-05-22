package com.thomas.guessthelink;

import jakarta.persistence.*;

@Entity
@Table(name = "rejected_answers")
public class RejectedAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String answer;

    @Column(length = 500)
    private String reason;

    @Column(length = 200)
    private String theme;

    @Column(name = "is_used")          // ← explicit, no more guessing
    private Boolean isUsed;            // ← Boolean (capital B), not boolean primitive

    public RejectedAnswer() {}

    public RejectedAnswer(String answer, String reason, String theme) {
        this.answer = answer;
        this.reason = reason;
        this.theme = theme;
        this.isUsed = false;
    }

    public RejectedAnswer(String answer, String theme, boolean isUsed) {
        this.answer = answer;
        this.theme = theme;
        this.isUsed = isUsed;
        this.reason = "Already used in game";
    }

    public Long getId()       { return id; }
    public String getAnswer() { return answer; }
    public String getReason() { return reason; }
    public String getTheme()  { return theme; }
    public Boolean isUsed()   { return isUsed; }
}
