package com.elorankingbot.backend.commands.player;

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
				game.getQueues().values().stream()
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
					queue.getPlayersPerTeam() : queue.getMaxPremadeSize();
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
		Game game = server.getGames().get(event.getOptions().get(0).getName());
		MatchFinderQueue queue;
		List<User> allyUsers;
		var gameOptions = event.getOptions().get(0).getOptions();
		// queue name is not in options
		if (gameOptions.isEmpty() || gameOptions.get(0).getValue().isPresent()) {
			queue = game.getQueues().values().stream().findAny().get();
			allyUsers = gameOptions.stream()
					.map(option -> option.getValue().get().asUser().block())
					.collect(Collectors.toList());
			// queue present in options
		} else {
			queue = game.getQueues().get(gameOptions.get(0).getName());
			allyUsers = gameOptions.get(0).getOptions().stream()
					.map(option -> option.getValue().get().asUser().block())
					.collect(Collectors.toList());
		}

		allyUsers.add(event.getInteraction().getUser());
		// TODO! accept-buttons
		Group group = new Group(
				allyUsers.stream()
						.map(user -> new Player(guildId, user.getId().asLong(), user.getTag()))
						.collect(Collectors.toList()),
				game);
		queue.addGroup(group);
		Optional<Match> maybeMatch = null;// TODO!
		service.saveServer(server);


		// schauen ob q voll
		// pruefen ob player doppelt in der q
		// q leeren, "match" generieren
		// spieler messages bauen
		// bei den buttoncommands weitermachen

	}
}
