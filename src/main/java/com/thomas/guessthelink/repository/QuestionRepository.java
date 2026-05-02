package com.thomas.guessthelink.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.thomas.guessthelink.Question;

//// A JPA-powered remote control for the Questions table,
//  using @Id (Long) as the key to find, save, and delete rows — 
// with custom queries like findByLevelNumber auto-generated from method names.
public interface QuestionRepository extends JpaRepository<Question, Long> {
    Question findByLevelNumber(int levelNumber);
}
