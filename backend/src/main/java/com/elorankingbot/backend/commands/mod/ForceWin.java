package com.elorankingbot.backend.commands.mod;

import com.elorankingbot.backend.command.ModCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.service.MatchService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;

import java.util.*;

import static discord4j.core.object.command.ApplicationCommandOption.Type.*;

@ModCommand
public class ForceWin extends SlashCommand {

	private List<List<Player>> teams;
	private Match match;
	private MatchFinderQueue queue;
	private List<User> users;

	public ForceWin(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		var requestBuilder = ApplicationCommandRequest.builder()
				.name(ForceWin.class.getSimpleName().toLowerCase())
				.description(getShortDescription())
				.defaultPermission(false);
		server.getGames().forEach(game -> {
			if (game.getQueues().size() == 1) {
				var queue = game.getQueues().stream().findAny().get();
				var queueOptionBuilder = ApplicationCommandOptionData.builder()
						.name(game.getName().toLowerCase()).description(queue.getDescription())
						.type(SUB_COMMAND.getValue());
				addUserOptions(queue, queueOptionBuilder);
				requestBuilder.addOption(queueOptionBuilder.build());
			} else {
				var gameOptionBuilder = ApplicationCommandOptionData.builder()
						.name(game.getName().toLowerCase()).description("game name")
						.type(SUB_COMMAND_GROUP.getValue());
				game.getQueues().forEach(queue -> {
					var queueOptionBuilder = ApplicationCommandOptionData.builder()
							.name(queue.getName().toLowerCase()).description(queue.getDescription())
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
				String optionName = queue.getNumPlayersPerTeam() == 1 ?
						teamIndex == 1 ? "winner" : "loser"
						: String.format("%s-team%s-player-%s",
						teamIndex == 1 ? "winning" : "losing",
						queue.getNumTeams() == 2 ? "" : "-" + teamIndex,
						playerIndex);
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

	protected void execute() {
		Game game = server.getGame(event.getOptions().get(0).getName());
		boolean isSingularQueue = event.getOptions().get(0).getOptions().get(0).getType().equals(USER);
		queue = isSingularQueue ? game.getQueues().stream().findAny().get()
				: game.getQueue(event.getOptions().get(0).getOptions().get(0).getName());
		var userOptions = isSingularQueue ? event.getOptions().get(0).getOptions()
				: event.getOptions().get(0).getOptions().get(0).getOptions();
		users = userOptions.stream().map(option -> option.getValue().get().asUser().block()).toList();
		for (User user : users) {
			if (user.isBot()) {
				event.reply(String.format("%s is a bot and cannot be part of a match.", user.getTag())).subscribe();
				return;
			}
		}
		if (users.size() > new HashSet<>(users).size()) {
			event.reply("A user cannot be in a match more than once.").subscribe();
			return;
		}

		teams = makeTeams();
		match = new Match(queue, teams);
		doReports();
		MatchResult matchResult = MatchService.generateMatchResult(match);
		String resolveMessage = String.format(String.format("%s has force-resolved a match of %s.",
				event.getInteraction().getUser().getTag(), game.getName()));
		EmbedCreateSpec matchEmbed = matchService.processForcedMatchResult(matchResult, users, resolveMessage);
		event.reply().withEmbeds(matchEmbed).subscribe();
	}

	private List<List<Player>> makeTeams() {
		List<List<Player>> result = new ArrayList<>(queue.getNumTeams());
		for (int teamIndex = 0; teamIndex < queue.getNumTeams(); teamIndex++) {
			result.add(new ArrayList<>(queue.getNumPlayersPerTeam()));
			for (int playerIndex = 0; playerIndex < queue.getNumPlayersPerTeam(); playerIndex++) {
				User user = users.get(teamIndex * queue.getNumPlayersPerTeam() + playerIndex);
				result.get(teamIndex).add(dbService.getPlayerOrGenerateIfNotPresent(
						server.getGuildId(), user.getId().asLong(), user.getTag()));
			}
		}
		return result;
	}

	private void doReports() {
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
