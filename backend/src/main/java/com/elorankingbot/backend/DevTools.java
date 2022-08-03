package com.elorankingbot.backend;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.commands.admin.CreateRanking;
import com.elorankingbot.backend.commands.admin.SetPermission;
import com.elorankingbot.backend.commands.admin.settings.Settings;
import com.elorankingbot.backend.commands.mod.Ban;
import com.elorankingbot.backend.commands.mod.RevertMatch;
import com.elorankingbot.backend.commands.player.Help;
import com.elorankingbot.backend.commands.player.Leave;
import com.elorankingbot.backend.commands.player.PlayerInfo;
import com.elorankingbot.backend.commands.player.QueueStatus;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.dao.*;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.GatewayDiscordClient;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DevTools {

	private final DBService dbService;
	private final DiscordBotService bot;
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

	private void updateGuildCommands() {
		log.warn("updating global commands...");
		bot.getAllGuildIds().stream().forEach(System.out::println);
		/*
		applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), Help.getRequest()).subscribe();
		applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), Settings.getRequest()).subscribe();
		applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), CreateRanking.getRequest()).subscribe();
		applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), Ban.getRequest()).subscribe();
		applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), Leave.getRequest()).subscribe();
		applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), PlayerInfo.getRequest()).subscribe();

		 */
		//applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), SetPermission.getRequest()).subscribe();
		//applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), RevertMatch.getRequest()).subscribe();
		log.warn("updating guild commands...");
		dbService.findAllServers().forEach(
				server -> {
					try {
						/*
						bot.deleteCommand(server, "help").subscribe();
						bot.deleteCommand(server, "settings").subscribe();
						bot.deleteCommand(server, "createranking").subscribe();
						bot.deleteCommand(server, "ban").subscribe();
						bot.deleteCommand(server, "leave").subscribe();
						bot.deleteCommand(server, "playerinfo").subscribe();

						 */

						//bot.deleteCommand(server, "setpermissions").subscribe();
						//bot.deleteCommand(server, "Revert Match").subscribe();

						//log.info("deploying to " + bot.getServerName(server));
						//bot.deployCommand(server, ForceWin.getRequest(server)).block();
						//bot.deployCommand(server, ForceDraw.getRequest(server)).block();
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
		);
	}
}
