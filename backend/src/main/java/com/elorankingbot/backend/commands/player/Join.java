package com.elorankingbot.backend.commands.player;

import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.service.QueueService;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.Buttons;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static com.elorankingbot.backend.model.MatchFinderQueue.QueueType.PREMADE;
import static com.elorankingbot.backend.model.MatchFinderQueue.QueueType.SOLO;
import static discord4j.core.object.command.ApplicationCommandOption.Type.*;

public class Join extends SlashCommand {

	private final QueueService queueService;
	private MatchFinderQueue queue;
	private Game game;
	private Match match;
	private List<User> allyUsers;

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
		game = server.getGames().get(event.getOptions().get(0).getName());
		boolean isSingularQueue;
		var gameOptions = event.getOptions().get(0).getOptions();
		// queue name is not in options
		if (gameOptions.isEmpty() || gameOptions.get(0).getValue().isPresent()) {
			queue = game.getQueues().values().stream().findAny().get();
			allyUsers = gameOptions.stream()
					.map(option -> option.getValue().get().asUser().block())
					.collect(Collectors.toList());
			isSingularQueue = true;
			// queue name present in options
		} else {
			queue = game.getQueues().get(gameOptions.get(0).getName());
			allyUsers = gameOptions.get(0).getOptions().stream()
					.map(option -> option.getValue().get().asUser().block())
					.collect(Collectors.toList());
			isSingularQueue = false;
		}
		for (User user : allyUsers) {
			if (user.isBot()) {
				event.reply("Bots cannot be added to the queue.").subscribe();
				return;
			}
		}

		allyUsers.add(event.getInteraction().getUser());

		// TODO! accept-buttons
		Group group = new Group(
				allyUsers.stream()
						.map(user -> service.getPlayerOrGenerateIfNotPresent(guildId, user.getId().asLong(), user.getTag()))
						.collect(Collectors.toList()),
				game);
		for (Player player : group.getPlayers()) {
			if (queueService.isPlayerInQueue(player, queue)) {
				event.reply(String.format("The player %s is already in this queue an cannot be added a second time.",
								player.getTag()))
						.withEphemeral(true).subscribe();
				return;
			}
		}
		queue.addGroup(group);
		service.saveServer(server);
		Optional<Match> maybeMatch = queueService.generateMatchIfPossible(queue);

		event.reply(String.format("Queue %s joined.",
						isSingularQueue ? game.getName()
								: game.getName() + " " + queue.getName()))
				.withEphemeral(true).subscribe();
		if (maybeMatch.isEmpty()) return;
		else {
			match = maybeMatch.get();
			processMatch();
		}
	}

	private void processMatch() {
		for (Player player : match.getPlayers()) {
			queueService.removePlayerFromAllQueues(server, player);
		}
		service.saveServer(server);
		sendPlayerMessages();
		// buttons:
		// accept
		// win
		// lose
		// draw
		// cancel
		// dispute
		// evtl kommentar bzgl self add entfernen...
		// TODO!

	}

	private void sendPlayerMessages() {
		List<List<Player>> teams = match.getGroups();
		List<Player> allPlayers = match.getPlayers();
		Set<Long> allyUserIds = allyUsers.stream().map(user -> user.getId().asLong()).collect(Collectors.toSet());
		Set<Long> allUserIds = allPlayers.stream().map(player -> player.getUserId()).collect(Collectors.toSet());
		List<User> allUsers = allyUsers;
		List<Mono<User>> userMonos = new ArrayList<>(allUserIds.size() - allyUserIds.size());
		for (long userId : allUserIds) {
			if (!allyUserIds.contains(userId)) {
				Mono<User> userMono = client.getUserById(Snowflake.of(userId));
				userMonos.add(userMono);
				userMono.subscribe(allUsers::add);
			}
		}
		Mono.when(userMonos).block();

		List<String> embedTexts = new ArrayList<>();
		for (List<Player> players : teams) {
			String embedText = "";
			for (Player player : players) {
				embedText += String.format("%s (%s)\n", player.getTag(), player.getRatings().get(game.getName()).getValue());
			}
			embedTexts.add(embedText);
		}

		for (User user : allUsers) {
			var embedBuilder = EmbedCreateSpec.builder()
					.title(String.format("Your match of %s %s is starting. " +
									"I removed you from all other queues you joined on this server, if any. " + // TODO auflisten welche queues
									"Please play the match and come back to report the result afterwards.",
							match.getQueue().getGame().getName(),
							match.getQueue().getGame().getName()));
			for (int i = 0; i < queue.getNumTeams(); i++) {
				embedBuilder.addField(EmbedCreateFields.Field.of(
								"Team #" + (i + 1),
								embedTexts.get(i).replace(user.getTag(), "**" + user.getTag() + "**"),
								true));
			}
			bot.getPrivateChannelByUserId(user.getId().asLong())
					.subscribe(privateChannel -> privateChannel
							.createMessage(embedBuilder.build())
							.withComponents(createActionRow(match.getId(), game.isAllowDraw())).subscribe());
		}
	}

	private static ActionRow createActionRow(UUID matchId, boolean allowDraw) {
		if (allowDraw) return ActionRow.of(
				Buttons.win(matchId),
				Buttons.lose(matchId),
				Buttons.draw(matchId),
				Buttons.cancel(matchId));
		else return ActionRow.of(
				Buttons.win(matchId),
				Buttons.lose(matchId),
				Buttons.cancel(matchId));
	}
}
