package com.elorankingbot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "elorankingbot")
public class ApplicationPropertiesLoader {

	@Getter
	@Setter
	public static class Patreon {

		private long commandId;
		private String redirectUri, campaignUrl, campaignId;
		private int supporterMinPledgeInDollars;
	}

	private Patreon patreon;
	private long ownerId;
	private long ente2Id;
	private long announcementChannelId;
	private int numberOfTimeSlots;
	private boolean useDevBotToken;
	private boolean doRunSchedulers;
	@Value("${spring.data.mongodb.database}")
	private String springDataMongodbDatabase;
	private String activityMessage;
	@Value("#{'${elorankingbot.test-server-ids}'.split(',')}")
	private List<Long> testServerIds;
}