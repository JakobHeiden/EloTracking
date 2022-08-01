package com.elorankingbot.backend.commands.admin.deleteranking;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.commands.admin.*;
import com.elorankingbot.backend.commands.mod.ForceDraw;
import com.elorankingbot.backend.commands.mod.ForceWin;
import com.elorankingbot.backend.commands.player.Join;
import com.elorankingbot.backend.commands.player.Leave;
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
		bot.deleteChannel(game.getLeaderboardChannelId());
		bot.deleteChannel(game.getResultChannelId());
		event.getInteraction().getMessage().get().edit().withComponents(none).subscribe();
		event.reply(String.format("Ranking %s deleted.", game.getName())).subscribe();

		bot.updateGuildCommandsByRanking(server);
		if (!game.getQueues().isEmpty()) {
			bot.updateGuildCommandsByQueue(server);
		}
	}

	private void deleteRatingsFromPlayers() {
		List<Player> players = dbService.findAllPlayersForServer(server);
		for (Player player : players) {
			player.deleteGameStats(game);
		}
		dbService.saveAllPlayers(players);
	}
}
