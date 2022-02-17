package com.elorankingbot.backend.commands.player;

import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.MatchFinderQueue;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.*;

import static discord4j.core.object.command.ApplicationCommandOption.Type.*;
import static com.elorankingbot.backend.model.MatchFinderQueue.QueueType.*;

public class Join extends SlashCommand {

	public Join(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
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


		// guards
		// queue holen
		// in der q ablegen
		// schauen ob q voll
		// q leeren, "match" generieren
		// spieler messages bauen
		// bei den buttoncommands weitermachen

	}
}
