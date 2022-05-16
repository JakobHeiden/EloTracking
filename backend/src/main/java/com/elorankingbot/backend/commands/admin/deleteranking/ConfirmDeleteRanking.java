package com.elorankingbot.backend.commands.admin.deleteranking;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.commands.admin.*;
import com.elorankingbot.backend.commands.player.Join;
import com.elorankingbot.backend.commands.player.Leave;
import com.elorankingbot.backend.commands.player.PlayerInfo;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

import java.util.List;

public class ConfirmDeleteRanking extends ButtonCommand {

	private Server server;
	private Game game;

	public ConfirmDeleteRanking(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		long userIdWhoClicked = event.getInteraction().getUser().getId().asLong();
		long userIdWhoCalledDeleteRanking = Long.parseLong(event.getCustomId().split(":")[2]);
		if (userIdWhoClicked != userIdWhoCalledDeleteRanking) {
			event.reply("Only the user who used `/deleteranking` can use this button.")
					.withEphemeral(true).subscribe();
			return;
		}

		server = dbService.findServerByGuildId(event.getInteraction().getGuildId().get().asLong()).get();
		game = server.getGame(event.getCustomId().split(":")[1]);
		deleteRatingsFromPlayers();
		dbService.deleteAllRankingsEntries(game);
		dbService.deleteAllMatches(game);
		dbService.deleteAllMatchResults(game);
		server.removeGame(game);
		dbService.saveServer(server);
		updateCommands();
		bot.deleteChannel(game.getLeaderboardChannelId());
		bot.deleteChannel(game.getResultChannelId());
		event.getInteraction().getMessage().get().edit().withComponents(none).subscribe();
		event.reply(String.format("Ranking %s deleted.", game.getName())).subscribe();
	}

	private void deleteRatingsFromPlayers() {
		List<Player> players = dbService.findAllPlayersForServer(server);
		for (Player player : players) {
			player.deleteGameStats(game);
		}
		dbService.saveAllPlayers(players);
	}

	private void updateCommands() {
		if (server.getGames().isEmpty()) {// TODO das hier generalisieren irgendwie... vllt ueber annotation
			bot.deleteCommand(server, AddQueue.class.getSimpleName().toLowerCase()).subscribe();
			bot.deleteCommand(server, AddRank.class.getSimpleName().toLowerCase()).subscribe();
			bot.deleteCommand(server, DeleteQueue.class.getSimpleName().toLowerCase()).subscribe();
			bot.deleteCommand(server, DeleteRanking.class.getSimpleName().toLowerCase()).subscribe();
			bot.deleteCommand(server, DeleteRanks.class.getSimpleName().toLowerCase()).subscribe();
			bot.deleteCommand(server, Join.class.getSimpleName().toLowerCase()).subscribe();
			bot.deleteCommand(server, Leave.class.getSimpleName().toLowerCase()).subscribe();
		} else {
			bot.deployCommand(server, AddQueue.getRequest(server)).subscribe();
			bot.deployCommand(server, AddRank.getRequest(server)).subscribe();
			bot.deployCommand(server, DeleteQueue.getRequest(server)).subscribe();
			bot.deployCommand(server, DeleteRanking.getRequest(server)).subscribe();
			bot.deployCommand(server, DeleteRanks.getRequest(server)).subscribe();
			bot.deployCommand(server, Join.getRequest(server)).subscribe();
			bot.deployCommand(server, Leave.getRequest()).subscribe();
		}
	}
}
