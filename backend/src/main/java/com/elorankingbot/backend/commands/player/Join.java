package com.elorankingbot.backend.commands.player;

import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.MatchFinderQueue;
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

	public static ApplicationCommandRequest getRequest(MatchFinderQueue queue) {
		Game game = queue.getGame();
		ImmutableApplicationCommandOptionData.Builder dynamicOptionBuilder = ApplicationCommandOptionData.builder()
				.name(queue.getName()).description("queue name")
				.type(SUB_COMMAND.getValue());
		if (queue.getQueueType() != SOLO) {
			for (int allyPlayerIndex = 1; allyPlayerIndex < queue.getPlayersPerTeam(); allyPlayerIndex++) {
				dynamicOptionBuilder.addOption(ApplicationCommandOptionData.builder()
						.name("ally" + allyPlayerIndex).description("ally #" + allyPlayerIndex)
						.type(USER.getValue())
						.required(queue.getQueueType() == PREMADE)
						.build());
			}
		}

		return ApplicationCommandRequest.builder()
				.name("join")
				.description("Join a matchmaking queue")
				.defaultPermission(true)
				.addOption(ApplicationCommandOptionData.builder()
						.name(game.getName()).description("game name")
						.type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
						.required(true)
						.addOption(dynamicOptionBuilder.build())
						.build())
				.build();
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
