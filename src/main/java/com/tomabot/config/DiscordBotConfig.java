package com.tomabot.config;

import com.tomabot.discord.listener.CommandListener;
import com.tomabot.discord.listener.ReadyListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DiscordBotConfig {

    @Value("${discord.bot.token}")
    private String botToken;

    @Value("${discord.bot.activity:🍅 /start to focus}")
    private String activity;

    private final CommandListener commandListener;
    private final ReadyListener readyListener;

    @Bean
    public JDA jda() throws Exception {
        log.info("🍅 Starting TomaBot...");

        JDA jda = JDABuilder.createDefault(botToken)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing(activity))
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MEMBERS
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .addEventListeners(commandListener, readyListener)
                .build();

        jda.awaitReady();
        log.info("✅ TomaBot is ready! Connected to {} guilds", jda.getGuilds().size());

        return jda;
    }
}