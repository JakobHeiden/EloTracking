package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.service.QueueService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.elorankingbot.backend.model.MatchFinderQueue.QueueType.PREMADE;
import static com.elorankingbot.backend.model.MatchFinderQueue.QueueType.SOLO;
import static discord4j.core.object.command.ApplicationCommandOption.Type.*;

public class Join extends SlashCommand {

	private final QueueService queueService;
	private MatchFinderQueue queue;
	private Game game;
	private Match match;
	private List<User> users;
	private List<User> allUsers;
	private List<Player> allPlayers;

	public Join(ChatInputInteractionEvent event, Services services) {
		super(event, services);
		this.queueService = services.queueService;
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		var requestBuilder = ApplicationCommandRequest.builder()
				.name("join")
				.description("Join a matchmaking queue")
				.defaultPermission(true);
		server.getGames().values().forEach(game -> {
			if (game.getQueues().size() == 1) {
				var queue = game.getQueues().values().stream().findAny().get();
				var queueOptionBuilder = ApplicationCommandOptionData.builder()
						.name(game.getName()).description(queue.getDescription())
						.type(SUB_COMMAND.getValue());
				addUserOptions(queue, queueOptionBuilder);
				requestBuilder.addOption(queueOptionBuilder.build());
			} else {
				var gameOptionBuilder = ApplicationCommandOptionData.builder()
						.name(game.getName()).description("game name")
						.type(SUB_COMMAND_GROUP.getValue());
				game.getQueues().values()
						.forEach(queue -> {
							var queueOptionBuilder = ApplicationCommandOptionData.builder()
									.name(queue.getName()).description(queue.getDescription())
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
		if (queue.getQueueType() != SOLO) {
			int maxPlayersInPremade = queue.getQueueType() == PREMADE ?
					queue.getNumPlayersPerTeam() : queue.getMaxPremadeSize();
			for (int allyPlayerIndex = 1; allyPlayerIndex < maxPlayersInPremade; allyPlayerIndex++) {
				queueOptionBuilder.addOption(ApplicationCommandOptionData.builder()
						.name("ally" + allyPlayerIndex).description("ally #" + allyPlayerIndex)
						.type(USER.getValue())
						.required(queue.getQueueType() == PREMADE)
						.build());
			}
		}
	}

	public void execute() {
		game = server.getGames().get(event.getOptions().get(0).getName());
		boolean isSingularQueue;
		var gameOptions = event.getOptions().get(0).getOptions();
		// queue name is not in options
		if (gameOptions.isEmpty() || gameOptions.get(0).getValue().isPresent()) {
			queue = game.getQueues().values().stream().findAny().get();
			users = gameOptions.stream()
					.map(option -> option.getValue().get().asUser().block())// TODO geht das ohne block?
					.collect(Collectors.toList());
			isSingularQueue = true;
			// queue name present in options
		} else {
			queue = game.getQueues().get(gameOptions.get(0).getName());
			users = gameOptions.get(0).getOptions().stream()
					.map(option -> option.getValue().get().asUser().block())
					.collect(Collectors.toList());
			isSingularQueue = false;
		}
		for (User user : users) {
			if (user.isBot()) {
				event.reply("Bots cannot be added to the queue.").subscribe();
				return;
			}
		}
		users.add(activeUser);

		Group group = new Group(
				users.stream()
						.map(user -> dbService.getPlayerOrGenerateIfNotPresent(guildId, user.getId().asLong(), user.getTag()))
						.collect(Collectors.toList()),
				game);
		for (Player player : group.getPlayers()) {
			if (queueService.isPlayerInQueue(player, queue)) {
				event.reply(String.format("The player %s is already in this queue an cannot be added a second time.",
								player.getTag()))// TODO unterscheiden nach active player
						.withEphemeral(true).subscribe();
				return;
			}
		}

		// TODO group queue

		queue.addGroup(group);
		dbService.saveServer(server);
		event.reply(String.format("Queue %s joined.",
						isSingularQueue ? game.getName()
								: game.getName() + " " + queue.getName()))
				.withEphemeral(true).subscribe();

		Optional<Match> maybeMatch = queueService.generateMatchIfPossible(queue);
		if (maybeMatch.isPresent()) {
			match = maybeMatch.get();
			matchService.startMatch(match, users);
		}
		// buttons:
		// accept
		// decline
		// -> leave
		// cancel
		// TODO!
	}
}
