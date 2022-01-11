package de.neuefische.elotracking.backend.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "elotracking")
public class ApplicationPropertiesLoader {

	private String ownerId;
	private long announcementChannelId;
	private String baseUrl;
	private int numberOfTimeSlots;
	private String testBotChallengerId;
	private String testBotAcceptorId;
	private String entenwieseId;
	private boolean useDevBotToken;
	private boolean deleteDataOnStartup;
	private boolean useGlobalCommands;
	private boolean setupDevGame;
	private boolean doUpdateGuildCommands;
	@Value("${spring.data.mongodb.database}")
	private String springDataMongodbDatabase;
}