package com.elorankingbot.backend.commands.player;

import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

public class ChallengeAsUserInteraction {

	private final UserInteractionEvent event;
	private final DBService service;
	private final DiscordBotService bot;
	private final TimedTaskQueue queue;

	public ChallengeAsUserInteraction(UserInteractionEvent event, Services services) {
		this.event = event;
		this.service = services.dbService;
		this.bot = services.bot;
		this.queue = services.queue;
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.type(2)
				.name("challenge")
				.build();
	}

	public void execute() {
		if (event.getTargetUser().block().isBot()) {
			event.reply("You cannot challenge a bot.").withEphemeral(true).subscribe();
			return;
		}

		Game game = service.findGameByGuildId(event.getInteraction().getGuildId().get().asLong()).get();
		long guildId = event.getInteraction().getGuildId().get().asLong();
		Challenge.staticExecute(
				event.getTargetUser().block(), guildId, game,
				event, service, bot, queue);
	}
}
