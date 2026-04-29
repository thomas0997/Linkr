package com.thomas.guessthelink.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thomas.guessthelink.GameProgress;


public interface GameProgressRepository extends JpaRepository<GameProgress, Long>
{
    
}
