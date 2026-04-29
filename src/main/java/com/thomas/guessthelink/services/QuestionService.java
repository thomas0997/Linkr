package com.thomas.guessthelink.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.thomas.guessthelink.Question;
import com.thomas.guessthelink.repository.QuestionRepository;
import java.util.List;

@Service
public class QuestionService {

    @Autowired //Spring automatically created QuestionRepository object and injected it here.
    // So no new QuestionRepository()
    private QuestionRepository questionRepo;


    // Gets Questions by level with arguments of int levelNumber from
    public Question getQuestionByLevel(int levelNumber) {
        return questionRepo.findByLevelNumber(levelNumber); 
        // Syntax questionRepo is built in with a method with findByLevelNumber with a param of levelnumber?
        // Spring Jpa built ins
    }

    public List<Question> getAllQuestions() {
        return questionRepo.findAll();
    }
}
