package com.elorankingbot.commands.mod;

import com.elorankingbot.command.annotations.ModCommand;
import com.elorankingbot.model.*;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;

import java.util.List;

import static discord4j.core.object.command.ApplicationCommandOption.Type.*;

@ModCommand
// Concerning command deployment, this is a special case dependent on isAllowDraw
// as such it is not annotated as @QueueCommand
// see DiscordCommandManager::updateQueueCommands
public class ForceDraw extends ForceMatch {


	public ForceDraw(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		var requestBuilder = ApplicationCommandRequest.builder()
				.name(ForceDraw.class.getSimpleName().toLowerCase())
				.description(getShortDescription())
				.defaultPermission(true);
		server.getGames()
				.stream().filter(Game::isAllowDraw)
				.forEach(game -> {
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
					optionName = "player-" + teamIndex;
				} else {
					optionName = String.format("team-%s-player-%s", teamIndex, playerIndex);
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
		return "Force a draw.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"This command will not be present unless the server is configured to have at least one ranking and one " +
				"queue. There will be one `/" + ForceDraw.class.getSimpleName().toLowerCase() + "` command for each queue" +
				"that belongs to a ranking that allows draws.\n" +
				"`Required:` Select the players for the match.";
	}

	protected void doReports() {
		for (List<Player> team : teams) {
			for (Player player : team) {
				match.reportAndSetConflictData(player.getId(), ReportStatus.DRAW);
			}
		}
	}
}
