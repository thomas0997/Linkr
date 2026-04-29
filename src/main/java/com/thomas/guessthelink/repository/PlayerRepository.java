package com.thomas.guessthelink.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thomas.guessthelink.Player;

public interface PlayerRepository extends JpaRepository<Player, Long>
{
    Player findByUsername(String username);

}
