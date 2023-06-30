package com.elorankingbot.commands.mod;

import com.elorankingbot.command.annotations.ModCommand;
import com.elorankingbot.command.annotations.QueueCommand;
import com.elorankingbot.model.MatchFinderQueue;
import com.elorankingbot.model.Player;
import com.elorankingbot.model.ReportStatus;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;

import java.util.List;

import static discord4j.core.object.command.ApplicationCommandOption.Type.*;

@ModCommand
@QueueCommand
public class ForceWin extends ForceMatch {

	public ForceWin(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		var requestBuilder = ApplicationCommandRequest.builder()
				.name(ForceWin.class.getSimpleName().toLowerCase())
				.description(getShortDescription())
				.defaultPermission(true);
		server.getGames().forEach(game -> {
			if (game.getQueues().size() == 1) {
				var queue = game.getQueues().stream().findAny().get();
				var queueOptionBuilder = ApplicationCommandOptionData.builder()
						.name(game.getName().toLowerCase()).description(getShortDescription())
						.type(SUB_COMMAND.getValue());
				addUserOptions(queue, queueOptionBuilder);
				requestBuilder.addOption(queueOptionBuilder.build());
			} else {
				var gameOptionBuilder = ApplicationCommandOptionData.builder()
						.name(game.getName().toLowerCase()).description("game name")
						.type(SUB_COMMAND_GROUP.getValue());
				game.getQueues().forEach(queue -> {
					var queueOptionBuilder = ApplicationCommandOptionData.builder()
							.name(queue.getName().toLowerCase()).description(getShortDescription())
							.type(SUB_COMMAND.getValue());
					addUserOptions(queue, queueOptionBuilder);
					gameOptionBuilder.addOption(queueOptionBuilder.build());
				});
				requestBuilder.addOption(gameOptionBuilder.build());
			}
		});
		return requestBuilder.build();
	}

	private static void addUserOptions(MatchFinderQueue queue, ImmutableApplicationCommandOptionData.Builder queueOptionBuilder) {
		for (int teamIndex = 1; teamIndex < queue.getNumTeams() + 1; teamIndex++) {
			for (int playerIndex = 1; playerIndex < queue.getNumPlayersPerTeam() + 1; playerIndex++) {
				String optionName;
				if (queue.getNumPlayersPerTeam() == 1) {
					if (queue.getNumTeams() == 2) {
						optionName = teamIndex == 1 ? "winner" : "loser";
					} else {
						optionName = teamIndex == 1 ? "player-1-winner" : String.format("player-%s-loser", teamIndex);
					}
				} else {
					if (queue.getNumTeams() == 2) {
						optionName = teamIndex == 1 ? "winning-team-player-" + playerIndex
								: "losing-team-player-" + playerIndex;
					} else {
						optionName = teamIndex == 1 ? "winning-team-1-player-" + playerIndex
								: String.format("losing-team-%s-player-%s", teamIndex, playerIndex);
					}
				}
				queueOptionBuilder.addOption(ApplicationCommandOptionData.builder()
						.name(optionName)
						.description(optionName)
						.type(USER.getValue())
						.required(true)
						.build());
			}
		}
	}

	public static String getShortDescription() {
		return "Force a win.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"This command will not be present unless the server is configured to have at least one ranking and one " +
				"queue. There will be one `/" + ForceWin.class.getSimpleName().toLowerCase() + "` command for each queue.\n" +
				"`Required:` Select the players for the match.";
	}

	protected void doReports() {
		for (Player player : teams.get(0)) {
			match.reportAndSetConflictData(player.getId(), ReportStatus.WIN);
		}
		for (List<Player> team : teams.subList(1, teams.size())) {
			for (Player player : team) {
				match.reportAndSetConflictData(player.getId(), ReportStatus.LOSE);
			}
		}
	}
}
