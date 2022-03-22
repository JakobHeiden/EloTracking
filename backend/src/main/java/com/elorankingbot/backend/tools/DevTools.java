package com.elorankingbot.backend.tools;

import com.elorankingbot.backend.commands.admin.CreateRanking;
import com.elorankingbot.backend.commands.admin.Edit;
import com.elorankingbot.backend.commands.admin.SetRole;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.dao.*;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.GatewayDiscordClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DevTools {

	private final DBService service;
	private final DiscordBotService bot;
	private final GatewayDiscordClient client;
	private final ApplicationPropertiesLoader props;
	private final PlayerDao playerDao;
	private final MatchDao matchDao;
	private final MatchResultDao matchResultDao;
	private final TimeSlotDao timeSlotDao;
	private final ServerDao serverDao;

	public DevTools(Services services,
					PlayerDao playerDao, MatchDao matchDao, MatchResultDao matchResultDao, TimeSlotDao timeSlotDao, ServerDao serverDao) {
		this.service = services.dbService;
		this.bot = services.bot;
		this.client = services.client;
		this.playerDao = playerDao;
		this.matchDao = matchDao;
		this.matchResultDao = matchResultDao;
		this.props = services.props;
		this.timeSlotDao = timeSlotDao;
		this.serverDao = serverDao;

		if (props.isDeleteDataOnStartup()) {
			deleteAllData();
			deployInitialCommands();
		}
		if (props.isDoUpdateGuildCommands()) updateGuildCommands();
	}

	private void updateGuildCommands() {
		log.warn("updating guild commands...");
		service.findAllServers().forEach(
				server -> {
					try {
						bot.deployCommand(server, Edit.getRequest(server)).block();
						bot.setAdminPermissionToAdminCommand(server, "edit");
						//Role adminRole = client.getRoleById(Snowflake.of(game.getGuildId()), Snowflake.of(game.getAdminRoleId())).block();
						//Role modRole = client.getRoleById(Snowflake.of(game.getGuildId()), Snowflake.of(game.getModRoleId())).block();
						//bot.setDiscordCommandPermissions(game.getGuildId(), "ban", adminRole, modRole);
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
		);
	}

	private void deployInitialCommands() {
		log.warn("Deploying initial guild commands...");
		long entenwieseId = props.getEntenwieseId();
		Server entenwieseServer = new Server(entenwieseId);
		service.saveServer(entenwieseServer);

		bot.deleteAllGuildCommands(entenwieseId).blockLast();
		bot.deployCommand(entenwieseServer, SetRole.getRequest()).block();
		bot.setCommandPermissionForRole(entenwieseServer, "setrole", entenwieseId);
		bot.deployCommand(entenwieseServer, CreateRanking.getRequest()).subscribe();
	}

	private void deleteAllData() {
		if (props.getSpringDataMongodbDatabase().equals("deploy")) {
			throw new RuntimeException("deleteAllData is being called on deploy database");
		}

		log.warn(String.format("Deleting all data on %s...", props.getSpringDataMongodbDatabase()));
		//challengeDao.deleteAll();
		matchDao.deleteAll();
		matchResultDao.deleteAll();
		playerDao.deleteAll();
		timeSlotDao.deleteAll();
		serverDao.deleteAll();
	}
}
