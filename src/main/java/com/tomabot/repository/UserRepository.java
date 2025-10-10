package com.tomabot.repository;

import com.tomabot.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByDiscordId(String discordId);

    boolean existsByDiscordId(String discordId);

}