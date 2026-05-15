package com.thomas.guessthelink.repository;

import com.thomas.guessthelink.RejectedAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RejectedAnswerRepository extends JpaRepository<RejectedAnswer, Long> {
    boolean existsByAnswerIgnoreCase(String answer);
    List<RejectedAnswer> findAll();
}