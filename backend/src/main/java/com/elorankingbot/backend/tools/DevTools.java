package com.elorankingbot.backend.tools;

import com.elorankingbot.backend.commands.admin.CreateGame;
import com.elorankingbot.backend.commands.admin.SetRole;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.dao.MatchDao;
import com.elorankingbot.backend.dao.PlayerDao;
import com.elorankingbot.backend.dao.ServerDao;
import com.elorankingbot.backend.dao.TimeSlotDao;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DevTools {

	private final EloRankingService service;
	private final DiscordBotService bot;
	private final GatewayDiscordClient client;
	private final ApplicationPropertiesLoader props;
	private final PlayerDao playerDao;
	private final MatchDao matchDao;
	private final TimeSlotDao timeSlotDao;
	private final ServerDao serverDao;

	public DevTools(EloRankingService service, DiscordBotService bot, GatewayDiscordClient client, PlayerDao playerDao,
					MatchDao matchDao, TimeSlotDao timeSlotDao, ServerDao serverDao) {
		this.service = service;
		this.bot = bot;
		this.client = client;
		this.playerDao = playerDao;
		this.matchDao = matchDao;
		this.props = service.getPropertiesLoader();
		this.timeSlotDao = timeSlotDao;
		this.serverDao = serverDao;

		if (props.isDeleteDataOnStartup()) deleteAllData();
		if (props.isDoUpdateGuildCommands()) updateGuildCommands();
	}

	private void updateGuildCommands() {
		log.warn("updating guild commands...");

		long entenwieseId = props.getEntenwieseId();
		Server entenwieseServer = new Server(entenwieseId);
		service.saveServer(entenwieseServer);

		bot.deleteAllGuildCommands(entenwieseId).blockLast();
		bot.deployCommand(entenwieseServer, SetRole.getRequest()).block();
		bot.setCommandPermissionForRole(entenwieseServer, "setrole", entenwieseId);
		bot.deployCommand(entenwieseServer, CreateGame.getRequest()).subscribe();

		service.findAllServers().forEach(
				server -> {
					try {

						//bot.deployCommand(game.getGuildId(), Info.getRequest()).block();
						//Role adminRole = client.getRoleById(Snowflake.of(game.getGuildId()), Snowflake.of(game.getAdminRoleId())).block();
						//Role modRole = client.getRoleById(Snowflake.of(game.getGuildId()), Snowflake.of(game.getModRoleId())).block();
						//bot.setDiscordCommandPermissions(game.getGuildId(), "ban", adminRole, modRole);
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
		);
	}

	private void deleteAllData() {
		if (props.getSpringDataMongodbDatabase().equals("deploy")) {
			throw new RuntimeException("deleteAllData is being called on deploy database");
		}

		log.info(String.format("Deleting all data on %s...", props.getSpringDataMongodbDatabase()));
		//challengeDao.deleteAll();
		//matchDao.deleteAll();
		//playerDao.deleteAll();
		timeSlotDao.deleteAll();
		serverDao.deleteAll();
	}
}
