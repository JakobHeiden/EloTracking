package com.elorankingbot.backend.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "elorankingbot")
public class ApplicationPropertiesLoader {

	private long ownerId;
	private long ente2Id;
	private long entenwieseId;
	private long announcementChannelId;
	private String baseUrl;
	private int numberOfTimeSlots;
	private String testBotChallengerId;
	private String testBotAcceptorId;
	private boolean useDevBotToken;
	private boolean deleteDataOnStartup;
	private boolean useGlobalCommands;
	private boolean setupDevGame;
	private boolean doUpdateGuildCommands;
	private boolean doRunQueue;
	@Value("${spring.data.mongodb.database}")
	private String springDataMongodbDatabase;
	private String activityMessage;
}