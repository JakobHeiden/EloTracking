package com.elorankingbot.backend.configuration;

import com.elorankingbot.backend.commands.Challenge;
import com.elorankingbot.backend.commands.ChallengeAsUserInteraction;
import com.elorankingbot.backend.commands.Forcematch;
import com.elorankingbot.backend.commands.Reset;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;

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
						bot.deleteAllGuildCommands(game.getGuildId()).blockLast();
						updateCommands(game).block();
						setPermissionsForAdminCommands(game);
						setPermissionsForModCommands(game);
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
		);
	}

	private Mono<Object> updateCommands(Game game) {
		long guildId = game.getGuildId();
		//Mono<Void> deleteSetup = bot.deleteCommand(guildId, Setup.getRequest().name());
		Mono<ApplicationCommandData> deployForcematch = bot.deployCommand(guildId, Forcematch.getRequest(game.isAllowDraw()));
		Mono<ApplicationCommandData> deployChallenge = bot.deployCommand(guildId, Challenge.getRequest());
		Mono<ApplicationCommandData> deployUserInteractionChallenge = bot.deployCommand(guildId, ChallengeAsUserInteraction.getRequest());
		Mono<ApplicationCommandData> deployReset = bot.deployCommand(guildId, Reset.getRequest());
		Mono<ApplicationCommandData> deployPermission = bot.deployCommand(guildId, com.elorankingbot.backend.commands.Permission.getRequest());
		return Mono.zip(deployForcematch, deployChallenge, deployUserInteractionChallenge,
				deployReset, deployPermission).map(allTheReturnValues -> "");
	}

	private void setPermissionsForAdminCommands(Game game) {
		long guildId = game.getGuildId();
		Role adminRole = client.getRoleById(Snowflake.of(guildId), Snowflake.of(game.getAdminRoleId())).block();
		Arrays.stream(com.elorankingbot.backend.commands.Permission.adminCommands)
				.forEach(commandName -> bot.setDiscordCommandPermissions(guildId, commandName, adminRole));
	}

	private void setPermissionsForModCommands(Game game) {
		long guildId = game.getGuildId();
		Role modRole = client.getRoleById(Snowflake.of(guildId), Snowflake.of(game.getModRoleId())).block();
		Role adminRole = client.getRoleById(Snowflake.of(guildId), Snowflake.of(game.getAdminRoleId())).block();
		Arrays.stream(com.elorankingbot.backend.commands.Permission.modCommands).forEach(
				commandName -> bot.setDiscordCommandPermissions(guildId, commandName, adminRole, modRole));
	}
}
