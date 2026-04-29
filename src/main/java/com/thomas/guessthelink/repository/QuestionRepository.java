package com.thomas.guessthelink.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.thomas.guessthelink.Question;


public interface QuestionRepository extends JpaRepository<Question, Long> {
    Question findByLevelNumber(int levelNumber);
}
