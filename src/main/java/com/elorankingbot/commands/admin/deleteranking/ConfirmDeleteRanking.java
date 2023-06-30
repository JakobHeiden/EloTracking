package com.elorankingbot.commands.admin.deleteranking;

import com.elorankingbot.commands.ButtonCommand;
import com.elorankingbot.model.Game;
import com.elorankingbot.model.Player;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

import java.util.List;

public class ConfirmDeleteRanking extends ButtonCommand {

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

		event.getInteraction().getMessage().get().edit().withComponents(none).subscribe();
		game = server.getGame(event.getCustomId().split(":")[1]);
		deleteRatingsFromPlayers();
		dbService.deleteAllRankingsEntries(game);
		dbService.deleteAllMatches(game);
		dbService.deleteAllMatchResults(game);
		server.removeGame(game);
		dbService.saveServer(server);
		bot.deleteChannel(game.getLeaderboardChannelId());
		bot.deleteChannel(game.getResultChannelId());
		String updatedCommands = discordCommandManager.updateGameCommands(server, exceptionHandler.updateCommandFailedCallbackFactory(event));
		if (!game.getQueues().isEmpty()) {
			updatedCommands += ", " + discordCommandManager.updateQueueCommands(server, exceptionHandler.updateCommandFailedCallbackFactory(event));
		}

		event.reply(String.format("Ranking %s deleted. These commands have been updated or deleted: %s" +
				"\nThis may take a minute to update on the server.",
				game.getName(), updatedCommands))
				.subscribe(NO_OP, super::forwardToExceptionHandler);
	}

	private void deleteRatingsFromPlayers() {
		List<Player> players = dbService.findAllPlayersForServer(server);
		for (Player player : players) {
			player.deleteGameStats(game);
		}
		dbService.saveAllPlayers(players);
	}
}
