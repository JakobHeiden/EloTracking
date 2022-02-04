package com.elorankingbot.backend.tools;

import com.elorankingbot.backend.commands.admin.Set;
import com.elorankingbot.backend.commands.admin.Setup;
import com.elorankingbot.backend.commands.mod.Ban;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DevTools {

	private final EloRankingService service;
	private final DiscordBotService bot;
	private final GatewayDiscordClient client;

	public DevTools(EloRankingService service, DiscordBotService bot, GatewayDiscordClient client) {
		this.service = service;
		this.bot = bot;
		this.client = client;

		ApplicationPropertiesLoader props = service.getPropertiesLoader();
		if (props.isDeleteDataOnStartup()) service.deleteAllData();
		if (props.isDoUpdateGuildCommands()) updateGuildCommands();

	}

	private void updateGuildCommands() {
		log.warn("updating guild commands...");
		service.findAllGames().forEach(
				game -> {
					try {
						log.info("updating " + game.getName());
						if (game.getLeaderboardLength() == 0) game.setLeaderboardLength(20);
						service.saveGame(game);
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
