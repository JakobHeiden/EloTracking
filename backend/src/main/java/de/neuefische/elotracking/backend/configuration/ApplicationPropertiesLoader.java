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

	private String adminId;
	private String baseUrl;
	private int numberOfTimeSlots;
	private String testBotChallengerId;
	private String testBotAcceptorId;
	private boolean useDevBotToken;
	private boolean deleteDataOnStartup;
	private boolean deployGuildCommands;
	private boolean deployGlobalCommands;
	private boolean setupDevGame;
	@Value("${spring.data.mongodb.database}")
	private String springDataMongodbDatabase;
}