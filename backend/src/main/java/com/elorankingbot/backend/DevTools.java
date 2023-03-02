package com.elorankingbot.backend;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.commands.admin.CreateRanking;
import com.elorankingbot.backend.commands.admin.SetPermission;
import com.elorankingbot.backend.commands.admin.settings.Settings;
import com.elorankingbot.backend.commands.mod.Ban;
import com.elorankingbot.backend.commands.mod.RevertMatch;
import com.elorankingbot.backend.commands.owner.AllGuilds;
import com.elorankingbot.backend.commands.owner.GuildInfo;
import com.elorankingbot.backend.commands.player.Leave;
import com.elorankingbot.backend.commands.player.PlayerInfo;
import com.elorankingbot.backend.commands.player.QueueStatus;
import com.elorankingbot.backend.commands.player.help.Help;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.dao.*;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.MatchFinderQueue;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.DiscordCommandService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.Channel;
import discord4j.rest.service.ApplicationService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@CommonsLog
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
        /*
        applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), RevertMatch.getRequest()).subscribe();
        applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), SetPermission.getRequest()).subscribe();
        applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), Help.getRequest()).subscribe();
        applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), Settings.getRequest()).subscribe();
        applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), CreateRanking.getRequest()).subscribe();
        applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), Ban.getRequest()).subscribe();
        applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), Leave.getRequest()).subscribe();
        applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), PlayerInfo.getRequest()).subscribe();
        applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), QueueStatus.getRequest()).subscribe();
         */
        log.warn("updating guild commands...");
        //applicationService.createGuildApplicationCommand(bot.getBotId(), 929504858585845810L, AllGuilds.getRequest()).subscribe();
        dbService.findAllServers().forEach(
                server -> {
                    try {
						if (props.getTestServerIds().contains(server.getGuildId())) {
							discordCommandService.deployCommand(server, GuildInfo.getRequest(),
									this::simplePrintThrowableCallback);
                            discordCommandService.deployCommand(server, AllGuilds.getRequest(),
                                    this::simplePrintThrowableCallback);
						}
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
        );
    }

    public Consumer<Throwable> simplePrintThrowableCallback(String ignored, Boolean alsoIgnored) {
        return System.out::println;
    }
}
