package com.elorankingbot.backend.tools;

import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.dao.MatchDao;
import com.elorankingbot.backend.dao.PlayerDao;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import discord4j.core.GatewayDiscordClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DevTools {

	private final EloRankingService service;
	private final DiscordBotService bot;
	private final GatewayDiscordClient client;
	private final PlayerDao playerDao;
	private final MatchDao matchDao;

	public DevTools(EloRankingService service, DiscordBotService bot, GatewayDiscordClient client, PlayerDao playerDao, MatchDao matchDao) {
		this.service = service;
		this.bot = bot;
		this.client = client;
		this.playerDao = playerDao;
		this.matchDao = matchDao;

		ApplicationPropertiesLoader props = service.getPropertiesLoader();
		if (props.isDeleteDataOnStartup()) service.deleteAllData();
		if (props.isDoUpdateGuildCommands()) updateGuildCommands();
	}

	private void updateGuildCommands() {
		log.warn("updating guild commands...");
		service.findAllGames().forEach(
				game -> {
					try {
						/*
						bot.deployCommand(game.getGuildId(), Set.getRequest()).block();
						Role adminRole = client.getRoleById(Snowflake.of(game.getGuildId()), Snowflake.of(game.getAdminRoleId())).block();
						Role modRole = client.getRoleById(Snowflake.of(game.getGuildId()), Snowflake.of(game.getModRoleId())).block();
						bot.setDiscordCommandPermissions(game.getGuildId(), "ban", adminRole, modRole);

						 */
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
		);
	}
}
