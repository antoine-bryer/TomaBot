package com.tomabot.service;

import com.tomabot.model.entity.User;
import com.tomabot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findByDiscordId(String discordId) {
        return userRepository.findByDiscordId(discordId).orElse(null);
    }

    @Transactional
    public User getOrCreateUser(String discordId, String username) {
        return userRepository.findByDiscordId(discordId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .discordId(discordId)
                            .username(username)
                            .isPremium(false)
                            .timezone("UTC")
                            .language("en")
                            .notificationsEnabled(true)
                            .build();

                    newUser = userRepository.save(newUser);
                    log.info("Created new user: {} ({})", username, discordId);
                    return newUser;
                });
    }

    @Transactional
    public User updateUsername(String discordId, String newUsername) {
        User user = userRepository.findByDiscordId(discordId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setUsername(newUsername);
        return userRepository.save(user);
    }
}