package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.Optional;

public class ChallengeAsUserInteraction {

	private final UserInteractionEvent event;
	private final EloRankingService service;
	private final DiscordBotService bot;
	private final TimedTaskQueue queue;
	private final GatewayDiscordClient client;// TODO kann weg?

	public ChallengeAsUserInteraction(UserInteractionEvent event, EloRankingService service, DiscordBotService bot,
									  TimedTaskQueue queue, GatewayDiscordClient client) {
		this.event = event;
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		this.client = client;
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.type(2)
				.name("challenge")
				.build();
	}

	public void execute() {
		Optional<Game> maybeGame = service.findGameByGuildId(event.getInteraction().getGuildId().get().asLong());
		if (maybeGame.isEmpty()) {
			event.reply("Please run /setup first.").withEphemeral(true).subscribe();
			return;
		}
		if (event.getTargetUser().block().isBot()) {
			event.reply("You cannot challenge a bot.").withEphemeral(true).subscribe();
			return;
		}

		long acceptorId = event.getTargetId().asLong();
		long guildId = event.getInteraction().getGuildId().get().asLong();
		Game game = maybeGame.get();
		Challenge.staticExecute(
				acceptorId, guildId, game,
				event, service, bot, queue);
	}
}
