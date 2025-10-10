package com.tomabot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.awt.Color;

@Service
@Slf4j
public class FocusModeService {

    private JDA jda;
    private static final String FOCUS_ROLE_NAME = "🍅 In Focus";

    @Autowired
    public void setJda(@Lazy JDA jda) {
        this.jda = jda;
    }

    public void enableFocusMode(String discordId) {
        jda.getGuilds().forEach(guild -> {
            Member member = guild.getMemberById(discordId);
            if (member != null) {
                addFocusRole(guild, member);
                muteInVoice(guild, member);
            }
        });
    }

    public void disableFocusMode(String discordId) {
        jda.getGuilds().forEach(guild -> {
            Member member = guild.getMemberById(discordId);
            if (member != null) {
                removeFocusRole(guild, member);
                unmuteInVoice(guild, member);
            }
        });
    }

    private void addFocusRole(Guild guild, Member member) {
        Role focusRole = getOrCreateFocusRole(guild);
        if (focusRole != null && !member.getRoles().contains(focusRole)) {
            guild.addRoleToMember(member, focusRole).queue(
                    success -> log.info("Added focus role to {} in {}",
                            member.getEffectiveName(), guild.getName()),
                    error -> log.warn("Failed to add focus role: {}", error.getMessage())
            );
        }
    }

    private void removeFocusRole(Guild guild, Member member) {
        Role focusRole = guild.getRolesByName(FOCUS_ROLE_NAME, true).stream()
                .findFirst()
                .orElse(null);

        if (focusRole != null && member.getRoles().contains(focusRole)) {
            guild.removeRoleFromMember(member, focusRole).queue(
                    success -> log.info("Removed focus role from {} in {}",
                            member.getEffectiveName(), guild.getName()),
                    error -> log.warn("Failed to remove focus role: {}", error.getMessage())
            );
        }
    }

    private void muteInVoice(Guild guild, Member member) {
        if (member.getVoiceState() != null && member.getVoiceState().inAudioChannel()) {
            guild.mute(member, true).queue(
                    success -> log.info("Muted {} in voice in {}",
                            member.getEffectiveName(), guild.getName()),
                    error -> log.warn("Failed to mute in voice: {}", error.getMessage())
            );
        }
    }

    private void unmuteInVoice(Guild guild, Member member) {
        if (member.getVoiceState() != null && member.getVoiceState().inAudioChannel()) {
            guild.mute(member, false).queue(
                    success -> log.info("Unmuted {} in voice in {}",
                            member.getEffectiveName(), guild.getName()),
                    error -> log.warn("Failed to unmute in voice: {}", error.getMessage())
            );
        }
    }

    private Role getOrCreateFocusRole(Guild guild) {
        Role existingRole = guild.getRolesByName(FOCUS_ROLE_NAME, true).stream()
                .findFirst()
                .orElse(null);

        if (existingRole != null) {
            return existingRole;
        }

        try {
            return guild.createRole()
                    .setName(FOCUS_ROLE_NAME)
                    .setColor(Color.decode("#FF6B6B"))
                    .setMentionable(false)
                    .setHoisted(true)
                    .complete();
        } catch (Exception e) {
            log.error("Failed to create focus role in guild {}: {}",
                    guild.getName(), e.getMessage());
            return null;
        }
    }
}