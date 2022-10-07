package com.elorankingbot.backend;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.commands.mod.SetRating;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.dao.*;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.DiscordCommandService;
import com.elorankingbot.backend.service.Services;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Role;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.Permission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class DevTools {

	private final DBService dbService;
	private final DiscordBotService bot;
	private final DiscordCommandService discordCommandService;
	private final GatewayDiscordClient client;
	private final ApplicationService applicationService;
	private final CommandClassScanner commandClassScanner;
	private final ApplicationPropertiesLoader props;
	private final PlayerDao playerDao;
	private final MatchDao matchDao;
	private final MatchResultDao matchResultDao;
	private final TimeSlotDao timeSlotDao;
	private final ServerDao serverDao;

	public DevTools(Services services, PlayerDao playerDao, MatchDao matchDao, MatchResultDao matchResultDao,
					TimeSlotDao timeSlotDao, ServerDao serverDao) {
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.discordCommandService = services.discordCommandService;
		this.client = services.client;
		this.applicationService = client.getRestClient().getApplicationService();
		this.commandClassScanner = services.commandClassScanner;
		this.playerDao = playerDao;
		this.matchDao = matchDao;
		this.matchResultDao = matchResultDao;
		this.props = services.props;
		this.timeSlotDao = timeSlotDao;
		this.serverDao = serverDao;

		if (props.isDoUpdateGuildCommands()) updateGuildCommands();
	}

	// Commands to deploy to production:
	private void updateGuildCommands() {
		log.warn("updating global commands...");
		//applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), SetPermission.getRequest()).subscribe();
		//applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), RevertMatch.getRequest()).subscribe();
		log.warn("updating guild commands...");
		//applicationService.createGuildApplicationCommand(bot.getBotId(), 929504858585845810L, AllGuilds.getRequest()).subscribe();
		dbService.findAllServers().forEach(
				server -> {
					try {
						log.info("deploying to " + bot.getServerName(server));
						//discordCommandService.deployCommand(server, SetRating.getRequest(server)).block();
						//bot.deployCommand(server, ForceDraw.getRequest(server)).block();
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
		);
	}
}
