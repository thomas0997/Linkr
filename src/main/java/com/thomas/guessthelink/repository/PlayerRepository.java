package com.thomas.guessthelink.repository;
 
import org.springframework.data.jpa.repository.JpaRepository;
import com.thomas.guessthelink.Player;
import java.util.*;
 
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Player findByUsernameIgnoreCase(String username);  // case-insensitive: "Admin" == "admin"
    List<Player> findAllByOrderByCurrentLevelDesc();
    long countByUsernameStartingWith(String prefix);
}
 